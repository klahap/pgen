package io.github.klahap.pgen

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.klahap.pgen.model.config.Config.Companion.buildConfig
import io.github.klahap.pgen.model.config.Config
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
import io.github.klahap.pgen.util.codegen.syncCodecs
import io.github.klahap.pgen.util.parseStatements
import io.github.klahap.pgen.util.toFlywayOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.io.path.createParentDirectories
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText


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
            )
        }
    }
    val spec = PgenSpec(
        tables = specData.flatMap(PgenSpec::tables).sorted(),
        enums = specData.flatMap(PgenSpec::enums).sorted(),
        compositeTypes = specData.flatMap(PgenSpec::compositeTypes).sorted(),
        statements = specData.flatMap(PgenSpec::statements).sortedBy(Statement::name),
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
        typeMappings = config.dbConfigs.flatMap(Config.Db::typeMappings)
            .associate { it.sqlType to it.valueClass },
        enumMappings = config.dbConfigs.flatMap(Config.Db::enumMappings)
            .associate { it.sqlType to it.enumClass },
        typeOverwrites = config.dbConfigs.flatMap(Config.Db::typeOverwrites)
            .associate { it.sqlColumn to it.valueClass },
        typeGroups = spec.tables.getColumnTypeGroups(),
        connectionType = config.connectionType,
        kotlinInstantType = config.kotlinInstantType,
    ).run {
        directorySync(config.outputPath) {
            DefaultCodeFile.all(connectionType = config.connectionType).forEach { sync(it) }
            spec.enums.forEach { sync(it) }
            syncCodecs(spec.enums)
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

private fun flywayMigration(config: Config) {
    config.dbConfigs.forEach { dbConfig ->
        val flyway = dbConfig.toFlywayOrNull() ?: return@forEach
        println("migrate db '${dbConfig.dbName}' with flyway")
        flyway.migrate()
    }
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
            flyway {
                migrationDirectory("./test_migration")
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
            enumMappings {
                add(sqlType = "public.role", clazz = "io.github.klahap.pgen_test.RoleDto")
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
        val testRepo = envFile["TEST_REPO"]
        outputPath("$testRepo/src/main/kotlin/db")
        specFilePath("$testRepo/src/main/resources/pgen-spec.yaml")
        createDirectoriesForRootPackageName(false)
        connectionType(Config.ConnectionType.R2DBC)
        kotlinInstantType(false)
    }
    generate(config)
}

@Suppress("unused")
class Plugin : Plugin<Project> {

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

        project.task("flywayMigration") { task ->
            task.group = TASK_GROUP
            task.doLast {
                val config = configBuilder.build()
                flywayMigration(config)
            }
        }
    }

    companion object {
        private const val TASK_GROUP = "quati tools"
    }
}
