package io.github.klahap.pgen

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.klahap.pgen.model.config.Config.Companion.buildConfig
import io.github.klahap.pgen.model.config.Config
import io.github.klahap.pgen.model.config.TypeMapping
import io.github.klahap.pgen.model.config.TypeOverwrite
import io.github.klahap.pgen.model.sql.Column
import io.github.klahap.pgen.model.sql.PgenSpec
import io.github.klahap.pgen.model.sql.Statement
import io.github.klahap.pgen.service.DbService
import io.github.klahap.pgen.service.DirectorySyncService.Companion.directorySync
import io.github.klahap.pgen.util.DefaultCodeFile
import io.github.klahap.pgen.service.EnvFileService
import io.github.klahap.pgen.util.codegen.CodeGenContext
import io.github.klahap.pgen.util.codegen.CodeGenContext.Companion.getColumnTypeGroups
import io.github.klahap.pgen.util.codegen.sync
import io.github.klahap.pgen.util.parseStatements
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.gradle.api.Project
import kotlin.io.path.*


private val yaml = Yaml(
    serializersModule = Yaml.default.serializersModule,
    configuration = YamlConfiguration(
        encodeDefaults = false,
        polymorphismStyle = PolymorphismStyle.Property,
    )
)

private fun generateSpec(config: Config) {
    val specData = config.dbConfigs.map { configDb ->
        DbService(
            dbName = configDb.dbName,
            connectionConfig = configDb.connectionConfig ?: error("no DB connection config defined")
        ).use { dbService ->
            val statements = dbService.getStatements(parseStatements(configDb.statementScripts))
            val tables = dbService.getTablesWithForeignTables(configDb.tableFilter)
            val allColumnTypes = tables.asSequence().flatMap { it.columns }.map { it.type }
                .map { if (it is Column.Type.NonPrimitive.Array) it.elementType else it }.toSet()
            val enumNames =
                allColumnTypes.filterIsInstance<Column.Type.NonPrimitive.Enum>().map { it.name }.toSet()
            val compositeTypeNames =
                allColumnTypes.filterIsInstance<Column.Type.NonPrimitive.Composite>().map { it.name }.toSet()
            val enums = dbService.getEnums(enumNames)
            val compositeTypes = dbService.getCompositeTypes(compositeTypeNames)
            PgenSpec(
                tables = tables,
                enums = enums,
                compositeTypes = compositeTypes,
                statements = statements,
                typeMappings = configDb.typeMappings.toList(),
                typeOverwrites = configDb.typeOverwrites.toList(),
            )
        }
    }
    val spec = PgenSpec(
        tables = specData.flatMap(PgenSpec::tables).sorted(),
        enums = specData.flatMap(PgenSpec::enums).sorted(),
        compositeTypes = specData.flatMap(PgenSpec::compositeTypes).sorted(),
        statements = specData.flatMap(PgenSpec::statements).sortedBy(Statement::name),
        typeMappings = specData.flatMap(PgenSpec::typeMappings).sortedBy(TypeMapping::sqlType),
        typeOverwrites = specData.flatMap(PgenSpec::typeOverwrites).sortedBy(TypeOverwrite::sqlColumn),
    )
    config.specFilePath.createParentDirectories().writeText(yaml.encodeToString(spec))
}

private fun generateCode(config: Config) {
    if (config.specFilePath.notExists())
        error("Pgen spec file does not exist: '${config.specFilePath}'")
    val spec = yaml.decodeFromString<PgenSpec>(config.specFilePath.readText())
    CodeGenContext(
        rootPackageName = config.packageName,
        createDirectoriesForRootPackageName = config.createDirectoriesForRootPackageName,
        typeMappings = spec.typeMappings.associate { it.sqlType to it.valueClass },
        typeOverwrites = spec.typeOverwrites.associate { it.sqlColumn to it.valueClass },
        typeGroups = spec.tables.getColumnTypeGroups()
    ).run {
        directorySync(config.outputPath) {
            DefaultCodeFile.all().forEach { sync(it) }
            spec.enums.forEach { sync(it) }
            spec.compositeTypes.forEach { sync(it) }
            spec.domains.filter { it.name !in typeMappings }.forEach { sync(it) }
            spec.tables.map { it.update() }.forEach { sync(it) }
            spec.statements.groupBy { it.name.dbName }.values.forEach { sync(it) }
            cleanup()
        }
    }
}

private fun generate(config: Config) {
    generateSpec(config = config)
    generateCode(config = config)
}

fun main() {
    val envFile = EnvFileService(".env")
    val config = buildConfig {
        addDb("scy") {
            connectionConfig {
                url(envFile["DB_URL"])
                user(envFile["DB_USER"])
                password(envFile["DB_PASSWORD"])
            }
            tableFilter {
                addSchemas("public")
            }
            statements {
                //addScript("./test-queries.sql")
            }
            typeMappings {
                add(sqlType = "public.stripe_account_id", clazz = "io.github.klahap.pgen_test.StripeAccountId")
            }
            typeOverwrites {
                add(
                    sqlColumn = "public.foo.id",
                    clazz = "io.github.klahap.pgen_test.MyId",
                    parseFunction = "foo"
                )
            }
        }
        packageName("io.github.klahap.pgen_test.db")
        outputPath("/Users/klaus/repos/pgen-test/src/main/kotlin/db")
        specFilePath("/Users/klaus/repos/pgen-test/src/main/resources/pgen-spec.yaml")
        createDirectoriesForRootPackageName(false)
    }
    generate(config)
}


class Plugin : org.gradle.api.Plugin<Project> {

    override fun apply(project: Project) {
        val configBuilder = project.extensions.create("pgen", Config.Builder::class.java)

        project.task("pgenGenerate") { task ->
            task.group = TASK_GROUP
            task.doLast {
                val config = configBuilder.build()
                generate(config)
            }
        }

        project.task("pgenGenerateSpec") { task ->
            task.group = TASK_GROUP
            task.doLast {
                val config = configBuilder.build()
                generateSpec(config)
            }
        }

        project.task("pgenGenerateCode") { task ->
            task.group = TASK_GROUP
            task.doLast {
                val config = configBuilder.build()
                generateCode(config)
            }
        }
    }

    companion object {
        private const val TASK_GROUP = "quati tools"
    }
}
