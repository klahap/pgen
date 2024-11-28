package io.github.klahap.pgen

import io.github.klahap.pgen.model.Config.Companion.buildConfig
import io.github.klahap.pgen.model.sql.Table
import io.github.klahap.pgen.model.Config
import io.github.klahap.pgen.model.sql.Enum
import io.github.klahap.pgen.model.sql.PgenSpec
import io.github.klahap.pgen.service.DbService
import io.github.klahap.pgen.service.DirectorySyncService.Companion.directorySync
import io.github.klahap.pgen.util.DefaultCodeFile
import io.github.klahap.pgen.service.EnvFileService
import io.github.klahap.pgen.util.codegen.CodeGenContext
import io.github.klahap.pgen.util.codegen.sync
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import kotlin.io.path.*


private val json = Json {
    prettyPrint = true
    @OptIn(ExperimentalSerializationApi::class)
    prettyPrintIndent = "  "
}

private fun generateSpec(config: Config) {
    val specData = config.dbConfigs.map { configDb ->
        DbService(
            dbName = configDb.dbName,
            connectionConfig = configDb.connectionConfig ?: error("no DB connection config defined")
        ).use { dbService ->
            val tables = dbService.getTablesWithForeignTables(configDb.tableFilter)
            val enumNames = tables.asSequence().flatMap { it.columns }.map { it.type }
                .map { if (it is Table.Column.Type.Array) it.elementType else it }
                .filterIsInstance<Table.Column.Type.Enum>().map { it.name }.toSet()
            val enums = dbService.getEnums(enumNames)
            // TODO add/try view tables
            PgenSpec(tables = tables, enums = enums)
        }
    }
    val spec = PgenSpec(
        tables = specData.flatMap(PgenSpec::tables).sortedBy(Table::name),
        enums = specData.flatMap(PgenSpec::enums).sortedBy(Enum::name),
    )
    config.specFilePath.createParentDirectories().writeText(json.encodeToString(spec))
}

private fun generateCode(config: Config) {
    if (config.specFilePath.notExists())
        error("Pgen spec file does not exist: '${config.specFilePath}'")
    val spec = json.decodeFromString<PgenSpec>(config.specFilePath.readText())
    CodeGenContext(
        rootPackageName = config.packageName,
        createDirectoriesForRootPackageName = config.createDirectoriesForRootPackageName,
    ).run {
        directorySync(config.outputPath) {
            DefaultCodeFile.all().forEach { sync(it) }
            spec.enums.forEach { sync(it) }
            spec.tables.forEach { sync(it) }
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
        }
        packageName("io.github.klahap.pgen_test.db")
        outputPath("build/output/db")
        specFilePath("build/output/pgen-spec.json")
        createDirectoriesForRootPackageName(false)
    }
    generate(config)
}


class Plugin : org.gradle.api.Plugin<Project> {

    override fun apply(project: Project) {
        val configBuilder = project.extensions.create("pgen", Config.Builder::class.java)

        project.task("pgen generate") { task ->
            task.group = TASK_GROUP
            task.doLast {
                val config = configBuilder.build()
                generate(config)
            }
        }

        project.task("pgen generate spec") { task ->
            task.group = TASK_GROUP
            task.doLast {
                val config = configBuilder.build()
                generateSpec(config)
            }
        }

        project.task("pgen generate code") { task ->
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
