package io.github.klahap.pgen

import io.github.klahap.pgen.model.Config.Companion.buildConfig
import io.github.klahap.pgen.model.sql.Table
import io.github.klahap.pgen.model.Config
import io.github.klahap.pgen.service.DbService
import io.github.klahap.pgen.service.DirectorySyncService.Companion.directorySync
import io.github.klahap.pgen.util.DefaultCodeFile
import io.github.klahap.pgen.service.EnvFileService
import io.github.klahap.pgen.util.codegen.CodeGenContext
import io.github.klahap.pgen.util.codegen.sync
import org.gradle.api.Project

private fun generate(config: Config) {
    val (tables, enums) = DbService(config.dbConnectionConfig).use { dbService ->
        val tables = dbService.getTablesWithForeignTables(config.tableFilter)
        val enumNames = tables.asSequence().flatMap { it.columns }.map { it.type }
            .map { if (it is Table.Column.Type.Array) it.elementType else it }
            .filterIsInstance<Table.Column.Type.Enum>().map { it.name }.toSet()
        val enums = dbService.getEnums(enumNames)
        tables to enums
    }

    // TODO add/try view tables

    val context = CodeGenContext(
        rootPackageName = config.packageName,
        createDirectoriesForRootPackageName = config.createDirectoriesForRootPackageName,
    )
    with(context) {
        directorySync(config.outputPath) {
            DefaultCodeFile.all().forEach { sync(it) }
            enums.forEach { sync(it) }
            tables.forEach { sync(it) }
            cleanup()
        }
    }
}

fun main() {
    val envFile = EnvFileService(".env")
    val config = buildConfig {
        dbConnectionConfig(
            url = envFile["DB_URL"],
            user = envFile["DB_USER"],
            password = envFile["DB_PASSWORD"],
        )
        packageName("io.github.klahap.pgen_test.db")
        tableFilter {
            addSchemas("public")
        }
        outputPath("build/output")
        createDirectoriesForRootPackageName(false)
    }
    generate(config)
}


class Plugin : org.gradle.api.Plugin<Project> {

    override fun apply(project: Project) {
        val configBuilder = project.extensions.create("pgen", Config.Builder::class.java)

        project.task("pgen") { task ->
            task.group = TASK_GROUP
            task.doLast {
                val config = configBuilder.build()
                generate(config)
            }
        }
    }

    companion object {
        private const val TASK_GROUP = "quati tools"
    }
}
