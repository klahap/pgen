package io.github.klahap.pgen.service

import io.github.klahap.pgen.dsl.executeQuery
import io.github.klahap.pgen.model.*
import io.github.klahap.pgen.model.sql.*
import io.github.klahap.pgen.model.sql.Enum
import io.github.klahap.pgen.model.sql.Table.Column.Type
import io.github.klahap.pgen.model.sql.Table.Column.Type.*
import java.io.Closeable
import java.sql.DriverManager
import java.sql.ResultSet

class DbService(
    val dbName: DbName,
    connectionConfig: Config.Db.DbConnectionConfig
) : Closeable {
    private val connection = DriverManager.getConnection(
        "${connectionConfig.url}?prepareThreshold=0",
        connectionConfig.user,
        connectionConfig.password
    )

    fun getTablesWithForeignTables(filter: SqlObjectFilter): List<Table> {
        return buildList {
            var currentFilter = filter
            while (!currentFilter.isEmpty()) {
                addAll(getTables(currentFilter))
                val tablesNames = map { it.name }.toSet()
                val foreignTableNames = flatMap { t -> t.foreignKeys.map { it.targetTable } }.toSet()
                val missingTableNames = foreignTableNames - tablesNames
                currentFilter = SqlObjectFilter.Objects(missingTableNames)
            }
        }
    }

    private fun getTables(filter: SqlObjectFilter): List<Table> {
        if (filter.isEmpty()) return emptyList()
        val columns = getColumns(filter)
        val primaryKeys = getPrimaryKeys(filter)
        val foreignKeys = getForeignKeys(filter)
        val tableNames = columns.keys + primaryKeys.keys + foreignKeys.keys

        return tableNames.map { tableName ->
            Table(
                name = tableName,
                columns = columns[tableName] ?: emptyList(),
                primaryKey = primaryKeys[tableName],
                foreignKeys = foreignKeys[tableName] ?: emptyList(),
            )
        }
    }

    private fun ResultSet.getColumnType(
        udtNameOverride: String? = null,
        columnTypeCategoryOverride: String? = null,
    ): Type {
        val schema = dbName.toSchema(getString("column_type_schema")!!)
        val columnName = udtNameOverride ?: getString("column_type_name")!!
        val columnTypeCategory = columnTypeCategoryOverride ?: getString("column_type_category")!!
        if (columnName.startsWith("_")) return NonPrimitive.Array(
            getColumnType(
                udtNameOverride = columnName.removePrefix("_"),
                columnTypeCategoryOverride = getString("column_element_type_category")!!
            )
        )
        if (schema != dbName.schemaPgCatalog) return when (columnTypeCategory) {
            "E" -> Type.NonPrimitive.Enum(SqlObjectName(schema = schema, name = columnName))
            else -> error("Unknown column type '$columnTypeCategory' for column_type column type '$schema:$columnName'")
        }
        return when (columnName) {
            "bool" -> Primitive.BOOL
            "bytea" -> Primitive.BINARY
            "date" -> Primitive.DATE
            "int2" -> Primitive.INT2
            "int4" -> Primitive.INT4
            "int8" -> Primitive.INT8
            "int4range" -> Primitive.INT4RANGE
            "int8range" -> Primitive.INT8RANGE
            "int4multirange" -> Primitive.INT4MULTIRANGE
            "int8multirange" -> Primitive.INT8MULTIRANGE
            "interval" -> Primitive.INTERVAL
            "json" -> Primitive.JSON
            "jsonb" -> Primitive.JSONB
            "numeric" -> {
                val precision = getInt("numeric_precision").takeIf { !wasNull() }
                val scale = getInt("numeric_scale").takeIf { !wasNull() }
                if (precision != null && scale != null)
                    NonPrimitive.Numeric(precision = precision, scale = scale)
                else if (precision == null && scale == null)
                    Primitive.UNCONSTRAINED_NUMERIC
                else
                    error("invalid numeric type, precision: $precision, scale: $scale")
            }

            "text" -> Primitive.TEXT
            "time" -> Primitive.TIME
            "timestamp" -> Primitive.TIMESTAMP
            "timestamptz" -> Primitive.TIMESTAMP_WITH_TIMEZONE
            "uuid" -> Primitive.UUID
            "varchar" -> Primitive.VARCHAR
            else -> error("undefined udt_name '$columnName'")
        }
    }

    private fun getColumns(filter: SqlObjectFilter): Map<SqlObjectName, List<Table.Column>> {
        if (filter.isEmpty()) return emptyMap()
        return connection.executeQuery(
            """
            SELECT
                c.table_schema AS table_schema,
                c.table_name AS table_name,
                c.column_name AS column_name,
                c.is_nullable AS is_nullable,
                c.udt_schema AS column_type_schema,
                c.udt_name AS column_type_name,
                c.numeric_precision AS numeric_precision,
                c.numeric_scale AS numeric_scale,
                c.column_default AS column_default,
                ty.typcategory AS column_type_category,
                tye.typcategory AS column_element_type_category
            FROM information_schema.columns AS c
            JOIN pg_catalog.pg_namespace AS na
                ON c.udt_schema = na.nspname
            JOIN pg_catalog.pg_type AS ty
                ON ty.typnamespace = na.oid
                    AND ty.typname = c.udt_name
            LEFT JOIN pg_catalog.pg_type AS tye
                ON ty.typelem != 0
                    AND tye.oid = ty.typelem
            WHERE ${filter.toFilterString(schemaField = "c.table_schema", tableField = "c.table_name")};
            """
        ) { resultSet ->
            val tableName = SqlObjectName(
                schema = dbName.toSchema(resultSet.getString("table_schema")!!),
                name = resultSet.getString("table_name")!!,
            )
            tableName to Table.Column(
                name = Table.ColumnName(resultSet.getString("column_name")!!),
                type = resultSet.getColumnType(),
                isNullable = resultSet.getBoolean("is_nullable"),
                default = resultSet.getString("column_default")
            )
        }.groupBy({ it.first }, { it.second })
    }

    private fun getPrimaryKeys(filter: SqlObjectFilter): Map<SqlObjectName, Table.PrimaryKey> {
        data class PrimaryKeyColumn(val keyName: String, val columnName: Table.ColumnName, val idx: Int)

        if (filter.isEmpty()) return emptyMap()
        return connection.executeQuery(
            """
                SELECT 
                    tc.table_schema,
                    tc.table_name,
                    kcu.column_name,
                    kcu.constraint_name,
                    kcu.ordinal_position
                FROM information_schema.table_constraints AS tc
                JOIN information_schema.key_column_usage AS kcu 
                    ON tc.constraint_name = kcu.constraint_name
                        AND tc.table_schema = kcu.table_schema
                WHERE tc.constraint_type = 'PRIMARY KEY'
                    AND ${filter.toFilterString(schemaField = "tc.table_schema", tableField = "tc.table_name")};
            """
        ) { resultSet ->
            val table = SqlObjectName(
                schema = dbName.toSchema(resultSet.getString("table_schema")!!),
                name = resultSet.getString("table_name")!!,
            )
            table to PrimaryKeyColumn(
                keyName = resultSet.getString("constraint_name")!!,
                columnName = Table.ColumnName(resultSet.getString("column_name")!!),
                idx = resultSet.getInt("ordinal_position")
            )
        }.groupBy({ it.first }, { it.second })
            .mapValues { (table, columns) ->
                Table.PrimaryKey(
                    keyName = columns.map { it.keyName }.distinct().singleOrNull()
                        ?: error("multiple primary keys for table $table"),
                    columnNames = columns.sortedBy { it.idx }.map { it.columnName },
                )
            }
    }

    private fun getForeignKeys(filter: SqlObjectFilter): Map<SqlObjectName, List<Table.ForeignKey>> {
        data class ForeignKeyMetaData(
            val name: String,
            val sourceTable: SqlObjectName,
            val targetTable: SqlObjectName,
        )

        if (filter.isEmpty()) return emptyMap()
        return connection.executeQuery(
            """
             SELECT 
                tc.constraint_name as constraint_name,
                tc.table_schema AS source_schema,
                tc.table_name AS source_table,
                kcu.column_name AS source_column,
                ccu.table_schema AS target_schema,
                ccu.table_name AS target_table,
                ccu.column_name AS target_column
            FROM information_schema.table_constraints AS tc
            JOIN information_schema.key_column_usage AS kcu
                ON tc.constraint_name = kcu.constraint_name
                AND tc.table_schema = kcu.table_schema
            JOIN information_schema.constraint_column_usage AS ccu
                ON tc.constraint_name = ccu.constraint_name
                AND tc.table_schema = ccu.table_schema
            WHERE tc.constraint_type = 'FOREIGN KEY'
                AND ${filter.toFilterString(schemaField = "tc.table_schema", tableField = "tc.table_name")};
            """
        ) { resultSet ->
            val meta = ForeignKeyMetaData(
                name = resultSet.getString("constraint_name")!!,
                sourceTable = SqlObjectName(
                    schema = dbName.toSchema(resultSet.getString("source_schema")!!),
                    name = resultSet.getString("source_table")!!,
                ),
                targetTable = SqlObjectName(
                    schema = dbName.toSchema(resultSet.getString("target_schema")!!),
                    name = resultSet.getString("target_table")!!,
                ),
            )
            val ref = Table.ForeignKey.KeyPair(
                sourceColumn = Table.ColumnName(resultSet.getString("source_column")!!),
                targetColumn = Table.ColumnName(resultSet.getString("target_column")!!),
            )
            meta to ref
        }
            .groupBy({ it.first }, { it.second })
            .map { (meta, refs) ->
                val key = Table.ForeignKey(
                    name = meta.name,
                    targetTable = meta.targetTable,
                    references = refs.distinct(),
                )
                meta.sourceTable to key
            }.groupBy({ it.first }, { it.second })
    }

    fun getEnums(enumNames: Set<SqlObjectName>): List<Enum> {
        data class EnumField(val order: UInt, val label: String)

        val filter = SqlObjectFilter.Objects(enumNames)
        if (filter.isEmpty()) return emptyList()
        val enums = connection.executeQuery(
            """
                SELECT 
                    na.nspname as enum_schema, 
                    ty.typname as enum_name, 
                    en.enumsortorder as enum_value_order, 
                    en.enumlabel as enum_value_label
                FROM pg_catalog.pg_type as ty
                JOIN pg_catalog.pg_namespace as na
                    ON ty.typnamespace = na.oid
                JOIN pg_catalog.pg_enum as en
                    ON en.enumtypid = ty.oid
                WHERE typcategory = 'E'
                    AND ${filter.toFilterString(schemaField = "na.nspname", tableField = "ty.typname")};
            """
        ) { resultSet ->
            val name = SqlObjectName(
                schema = dbName.toSchema(resultSet.getString("enum_schema")!!),
                name = resultSet.getString("enum_name"),
            )
            val field = EnumField(
                order = resultSet.getInt("enum_value_order").takeIf { it > 0 }!!.toUInt(),
                label = resultSet.getString("enum_value_label")!!,
            )
            name to field
        }
            .groupBy({ it.first }, { it.second })
            .map { (name, fields) ->
                Enum(
                    name = name,
                    fields = fields.sortedBy { it.order }.map { it.label },
                )
            }
        val missingEnums = enumNames - enums.map { it.name }.toSet()
        if (missingEnums.isNotEmpty())
            throw Exception("enums not found: $missingEnums")
        return enums
    }

    override fun close() {
        connection.close()
    }
}