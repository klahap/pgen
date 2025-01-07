package io.github.klahap.pgen.model

import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.model.sql.DbName
import io.github.klahap.pgen.model.sql.SqlObjectFilter
import java.nio.file.Path
import kotlin.io.path.Path

data class Config(
    val dbConfigs: List<Db>,
    val packageName: PackageName,
    val outputPath: Path,
    val specFilePath: Path,
    val createDirectoriesForRootPackageName: Boolean,
) {
    data class Db(
        val dbName: DbName,
        val connectionConfig: DbConnectionConfig?,
        val tableFilter: SqlObjectFilter,
        val statementScripts: Set<Path>,
    ) {
        data class DbConnectionConfig(
            val url: String,
            val user: String,
            val password: String,
        ) {
            class Builder {
                private var url: String? = null
                private var user: String? = null
                private var password: String? = null

                fun url(value: String) = apply { url = value }
                fun user(value: String) = apply { user = value }
                fun password(value: String) = apply { password = value }
                fun build() = DbConnectionConfig(
                    url = url ?: error("invalid DB connection config, url not defined"),
                    user = user ?: error("invalid DB connection config, user not defined"),
                    password = password ?: error("invalid DB connection config, password not defined"),
                )
            }
        }

        class Builder(name: String) {
            private val dbName = DbName(name.also {
                if (it.isBlank()) error("empty DB name")
            })
            private var connectionConfig: DbConnectionConfig? = null
            private var tableFilter: SqlObjectFilter? = null
            private var statementScripts: Set<Path>? = null

            class StatementCollectionBuilder {
                private val scripts = linkedSetOf<Path>()
                fun addScript(file: Path) = apply { scripts.add(file) }
                fun addScript(file: String) = apply { scripts.add(Path(file)) }
                fun build() = scripts.toSet()
            }

            fun connectionConfig(ignoreErrors: Boolean = true, block: DbConnectionConfig.Builder.() -> Unit) = apply {
                this.connectionConfig = runCatching {
                    DbConnectionConfig.Builder().apply(block).build()
                }.getOrElse {
                    if (ignoreErrors)
                        return@apply
                    throw it
                }
            }

            fun tableFilter(block: SqlObjectFilter.Builder.() -> Unit) {
                tableFilter = SqlObjectFilter.Builder(dbName = dbName).apply(block).build()
            }

            fun statements(block: StatementCollectionBuilder.() -> Unit) {
                statementScripts = StatementCollectionBuilder().apply(block).build()
            }

            fun build() = Db(
                dbName = dbName,
                connectionConfig = connectionConfig,
                tableFilter = tableFilter ?: error("no table filter defined for DB config '$dbName'"),
                statementScripts = statementScripts ?: emptySet(),
            )
        }
    }

    open class Builder {
        private val dbConfigs: MutableList<Db> = mutableListOf()
        private var packageName: String? = null
        private var outputPath: Path? = null
        private var specFilePath: Path? = null
        private var createDirectoriesForRootPackageName: Boolean = true

        fun packageName(name: String) = apply { packageName = name }
        fun outputPath(path: String) = apply { outputPath = Path(path) }
        fun outputPath(path: Path) = apply { outputPath = path }
        fun specFilePath(path: String) = apply { specFilePath = Path(path) }
        fun specFilePath(path: Path) = apply { specFilePath = path }
        fun createDirectoriesForRootPackageName(value: Boolean) = apply { createDirectoriesForRootPackageName = value }
        fun addDb(name: String, block: Db.Builder.() -> Unit) {
            val db = Db.Builder(name = name).apply(block).build()
            dbConfigs.add(db)
        }

        fun build() = Config(
            dbConfigs = dbConfigs
                .also { dbs ->
                    val duplicateDbNames = dbs.map { it.dbName }.groupBy { it }.filterValues { it.size > 1 }.keys
                    if (duplicateDbNames.isNotEmpty())
                        error("Duplicate DB names $duplicateDbNames")
                }
                .takeIf { it.isNotEmpty() } ?: error("no DB config defined"),
            packageName = packageName?.let { PackageName(it) } ?: error("no output package defined"),
            outputPath = outputPath ?: error("no output path defined"),
            specFilePath = specFilePath ?: error("no path pgen spec file defined"),
            createDirectoriesForRootPackageName = createDirectoriesForRootPackageName,
        )
    }

    companion object {
        fun buildConfig(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
}