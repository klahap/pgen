package io.github.klahap.pgen.model

import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.model.sql.SqlObjectFilter
import java.nio.file.Path
import kotlin.io.path.Path

data class Config(
    val dbConnectionConfig: DbConnectionConfig?,
    val packageName: PackageName,
    val tableFilter: SqlObjectFilter,
    val outputPath: Path,
    val createDirectoriesForRootPackageName: Boolean,
) {
    data class DbConnectionConfig(
        val url: String,
        val user: String,
        val password: String,
    )

    open class Builder {
        private var dbConnectionConfig: DbConnectionConfig? = null
        private var packageName: String? = null
        private var tableFilter: SqlObjectFilter? = null
        private var outputPath: Path? = null
        private var createDirectoriesForRootPackageName: Boolean = true

        fun dbConnectionConfig(
            url: String,
            user: String,
            password: String,
        ) {
            dbConnectionConfig = DbConnectionConfig(url = url, user = user, password = password)
        }

        fun packageName(name: String) {
            packageName = name
        }

        fun outputPath(path: String) = outputPath(Path(path))
        fun outputPath(path: Path) {
            outputPath = path
        }

        fun tableFilter(block: SqlObjectFilter.Builder.() -> Unit) {
            tableFilter = SqlObjectFilter.Builder().apply(block).build()
        }

        fun createDirectoriesForRootPackageName(value: Boolean) {
            createDirectoriesForRootPackageName = value
        }

        fun build() = Config(
            dbConnectionConfig = dbConnectionConfig,
            packageName = packageName?.let { PackageName(it) } ?: error("no output package defined"),
            tableFilter = tableFilter ?: error("no table filter defined"),
            outputPath = outputPath ?: error("no output path defined"),
            createDirectoriesForRootPackageName = createDirectoriesForRootPackageName,
        )
    }

    companion object {
        fun buildConfig(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
}