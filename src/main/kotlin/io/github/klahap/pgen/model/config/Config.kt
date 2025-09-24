package io.github.klahap.pgen.model.config

import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.model.sql.DbName
import io.github.klahap.pgen.model.sql.KotlinClassName
import io.github.klahap.pgen.model.sql.KotlinEnumClass
import io.github.klahap.pgen.model.sql.KotlinValueClass
import io.github.klahap.pgen.model.sql.SchemaName
import io.github.klahap.pgen.model.sql.SqlColumnName
import io.github.klahap.pgen.model.sql.SqlObjectName
import java.nio.file.Path
import kotlin.io.path.Path

data class Config(
    val dbConfigs: List<Db>,
    val packageName: PackageName,
    val outputPath: Path,
    val specFilePath: Path,
    val createDirectoriesForRootPackageName: Boolean,
    val connectionType: ConnectionType,
    val kotlinInstantType: Boolean, // TODO remove when Exposed v1 is stable
) {
    enum class ConnectionType {
        JDBC, R2DBC
    }

    data class Db(
        val dbName: DbName,
        val connectionConfig: DbConnectionConfig?,
        val tableFilter: SqlObjectFilter,
        val statementScripts: Set<Path>,
        val typeMappings: Set<TypeMapping>,
        val enumMappings: Set<EnumMapping>,
        val typeOverwrites: Set<TypeOverwrite>,
        val flyway: Flyway?,
    ) {
        data class Flyway(
            val migrationDirectory: Path,
        ) {
            class Builder {
                private var migrationDirectory: Path? = null
                fun migrationDirectory(path: Path) = apply { migrationDirectory = path }
                fun migrationDirectory(path: String) = apply { migrationDirectory = Path(path) }
                fun build() = Flyway(
                    migrationDirectory = migrationDirectory ?: error("no migration file directory defined"),
                )
            }
        }

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
            private var typeMappings: Set<TypeMapping>? = null
            private var enumMappings: Set<EnumMapping>? = null
            private var typeOverwrites: Set<TypeOverwrite>? = null
            private var flyway: Flyway? = null

            class StatementCollectionBuilder {
                private val scripts = linkedSetOf<Path>()
                fun addScript(file: Path) = apply { scripts.add(file) }
                fun addScript(file: String) = apply { scripts.add(Path(file)) }
                fun build() = scripts.toSet()
            }

            class TypeMappingBuilder(private val dbName: DbName) {
                private val mappings = linkedSetOf<TypeMapping>()
                fun add(
                    sqlType: String,
                    clazz: String,
                    parseFunction: String? = null,
                ) {
                    val (schemaName, name) = sqlType.takeIfValidAbsoluteClazzName(size = 2)?.split('.')
                        ?: throw IllegalArgumentException("illegal sqlType '$sqlType', expected format <schema>.<name>")
                    val entity = TypeMapping(
                        sqlType = SqlObjectName(
                            schema = SchemaName(dbName = dbName, schemaName = schemaName),
                            name = name,
                        ),
                        valueClass = KotlinValueClass(
                            name = clazz.toKotlinClassName(),
                            parseFunction = parseFunction?.takeIf(String::isNotBlank),
                        ),
                    )
                    mappings.add(entity)
                }

                fun build() = mappings.toSet()
            }

            class EnumMappingBuilder(private val dbName: DbName) {
                private val enumMappings = linkedSetOf<EnumMapping>()
                fun add(
                    sqlType: String,
                    clazz: String,
                    mappings: Map<String, String> = emptyMap(),
                ) {
                    val (schemaName, name) = sqlType.takeIfValidAbsoluteClazzName(size = 2)?.split('.')
                        ?: throw IllegalArgumentException("illegal sqlType '$sqlType', expected format <schema>.<name>")
                    val entity = EnumMapping(
                        sqlType = SqlObjectName(
                            schema = SchemaName(dbName = dbName, schemaName = schemaName),
                            name = name,
                        ),
                        enumClass = KotlinEnumClass(
                            name = clazz.toKotlinClassName(),
                            mappings = mappings,
                        ),
                    )
                    enumMappings.add(entity)
                }

                fun build() = enumMappings.toSet()
            }

            class TypeOverwriteBuilder(private val dbName: DbName) {
                private val overwrites = linkedSetOf<TypeOverwrite>()
                fun add(
                    sqlColumn: String,
                    clazz: String,
                    parseFunction: String? = null,
                ) {
                    val (schemaName, tableName, columnName) = sqlColumn.takeIfValidAbsoluteClazzName(size = 3)
                        ?.split('.')
                        ?: throw IllegalArgumentException("illegal column name '$sqlColumn', expected format <schema>.<table>.<name>")
                    val entity = TypeOverwrite(
                        sqlColumn = SqlColumnName(
                            tableName = SqlObjectName(
                                schema = SchemaName(dbName = dbName, schemaName = schemaName),
                                name = tableName,
                            ),
                            name = columnName,
                        ),
                        valueClass = KotlinValueClass(
                            name = clazz.toKotlinClassName(),
                            parseFunction = parseFunction?.takeIf(String::isNotBlank),
                        ),
                    )
                    overwrites.add(entity)
                }

                fun build() = overwrites.toSet()
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

            fun typeMappings(block: TypeMappingBuilder.() -> Unit) {
                typeMappings = TypeMappingBuilder(dbName = dbName).apply(block).build()
            }

            fun enumMappings(block: EnumMappingBuilder.() -> Unit) {
                enumMappings = EnumMappingBuilder(dbName = dbName).apply(block).build()
            }

            fun typeOverwrites(block: TypeOverwriteBuilder.() -> Unit) {
                typeOverwrites = TypeOverwriteBuilder(dbName = dbName).apply(block).build()
            }

            fun flyway(block: Flyway.Builder.() -> Unit) {
                flyway = Flyway.Builder().apply(block).build()
            }

            fun build() = Db(
                dbName = dbName,
                connectionConfig = connectionConfig,
                tableFilter = tableFilter ?: error("no table filter defined for DB config '$dbName'"),
                statementScripts = statementScripts ?: emptySet(),
                typeMappings = typeMappings?.distinctBy(TypeMapping::sqlType)?.toSet() ?: emptySet(),
                enumMappings = enumMappings?.distinctBy(EnumMapping::sqlType)?.toSet() ?: emptySet(),
                typeOverwrites = typeOverwrites?.distinctBy(TypeOverwrite::sqlColumn)?.toSet() ?: emptySet(),
                flyway = flyway,
            )

            companion object {
                private fun String.takeIfValidAbsoluteClazzName(size: Int? = null): String? {
                    val parts = split('.')
                    if (parts.any(String::isBlank)) return null
                    return if (size != null)
                        takeIf { parts.size == size }
                    else
                        takeIf { parts.size > 1 }
                }

                private fun String.toKotlinClassName(): KotlinClassName {
                    takeIfValidAbsoluteClazzName()
                        ?: throw IllegalArgumentException("illegal class name '$this', provide full class name with package")
                    return KotlinClassName(
                        packageName = substringBeforeLast('.'),
                        className = substringAfterLast('.'),
                    )
                }
            }
        }
    }

    open class Builder {
        private val dbConfigs: MutableList<Db> = mutableListOf()
        private var packageName: String? = null
        private var outputPath: Path? = null
        private var specFilePath: Path? = null
        private var createDirectoriesForRootPackageName: Boolean = true
        private var connectionType: ConnectionType = ConnectionType.JDBC
        private var kotlinInstantType: Boolean = true

        fun connectionType(type: ConnectionType) = apply { connectionType = type }
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

        fun kotlinInstantType(value: Boolean) = apply { kotlinInstantType = value }

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
            connectionType = connectionType,
            kotlinInstantType = kotlinInstantType,
        )
    }

    companion object {
        fun buildConfig(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
}