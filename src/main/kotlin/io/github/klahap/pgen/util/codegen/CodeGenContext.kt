package io.github.klahap.pgen.util.codegen

import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.model.config.Config
import io.github.klahap.pgen.model.sql.Column
import io.github.klahap.pgen.model.sql.KotlinValueClass
import io.github.klahap.pgen.model.sql.SqlColumnName
import io.github.klahap.pgen.model.sql.SqlObjectName
import io.github.klahap.pgen.model.sql.Table

class CodeGenContext(
    rootPackageName: PackageName,
    val createDirectoriesForRootPackageName: Boolean,
    val typeMappings: Map<SqlObjectName, KotlinValueClass>,
    typeOverwrites: Map<SqlColumnName, KotlinValueClass>,
    typeGroups: List<Set<SqlColumnName>>,
    val connectionType: Config.ConnectionType,
) {
    val allTypeOverwrites: Map<SqlColumnName, KotlinValueClass> = typeOverwrites.entries.flatMap { (column, clazz) ->
        val group = typeGroups.firstOrNull { it.contains(column) } ?: setOf(column)
        group.map { c -> c to clazz }
    }
        .groupBy({ it.first }, { it.second })
        .mapValues { it.value.toSet() }
        .mapValues {
            it.value.singleOrNull() ?: throw Exception("multiple type overwrites for ${it.key}: ${it.value}")
        }

    fun Table.update(): Table {
        val newColumns = columns.map { column ->
            val columnName = SqlColumnName(tableName = name, name = column.name.value)
            if (column.type is Column.Type.NonPrimitive.Reference) return@map column
            val kotlinClass = allTypeOverwrites[columnName] ?: return@map column
            val newType = Column.Type.NonPrimitive.Reference(
                valueClass = kotlinClass,
                originalType = when (val t = column.type) {
                    is Column.Type.NonPrimitive.Domain -> t.originalType
                    else -> t
                },
            )
            column.copy(type = newType)
        }
        return copy(columns = newColumns)
    }

    val poet = Poet(rootPackageName = rootPackageName)

    data class Poet(
        val rootPackageName: PackageName,
    ) {
        val packageCustomColumn = PackageName("$rootPackageName.column_type")
        private val packageUtil = PackageName("$rootPackageName.util")

        val getArrayColumnType = packageCustomColumn.className("getArrayColumnType")
        val domainType = packageCustomColumn.className("domainType")
        val domainTypeColumn = packageCustomColumn.className("DomainTypeColumn")
        val customEnumerationArray = packageCustomColumn.className("customEnumerationArray")
        val multiRange = packageCustomColumn.className("MultiRange")
        val defaultJsonColumnType = packageCustomColumn.className("DefaultJsonColumnType")
        val int4RangeColumnType = packageCustomColumn.className("Int4RangeColumnType")
        val int8RangeColumnType = packageCustomColumn.className("Int8RangeColumnType")
        val int4MultiRangeColumnType = packageCustomColumn.className("Int4MultiRangeColumnType")
        val int8MultiRangeColumnType = packageCustomColumn.className("Int8MultiRangeColumnType")
        val unconstrainedNumericColumnType = packageCustomColumn.className("UnconstrainedNumericColumnType")
        val pgStructFieldConverter = packageCustomColumn.className("PgStructFieldConverter")
        val pgStructField = packageCustomColumn.className("PgStructField")
        val pgStructFieldJoin = packageCustomColumn.className("join")
        val getColumnWithAlias = packageUtil.className("get")
        val pgEnum = packageCustomColumn.className("PgEnum")
        val getPgEnumByLabel = packageCustomColumn.className("getPgEnumByLabel")
        val toDbObject = packageCustomColumn.className("toDbObject")
        val stringLike = packageCustomColumn.className("StringLike")
    }

    companion object {

        fun Collection<Table>.getColumnTypeGroups(): List<Set<SqlColumnName>> {
            return flatMap { table ->
                table.foreignKeys.flatMap { keySet ->
                    keySet.references.map { reference ->
                        SqlColumnName(
                            tableName = table.name,
                            name = reference.sourceColumn.value
                        ) to SqlColumnName(
                            tableName = keySet.targetTable,
                            name = reference.targetColumn.value
                        )
                    }
                }
            }.flatMap { (a, b) -> listOf(a to b, b to a) }
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                .map { it.value.toSet() + setOf(it.key) }
                .mergeIntersections()
        }

        private fun <T> List<Set<T>>.mergeIntersections(): List<Set<T>> {
            val result = mutableListOf<Set<T>>()
            var remaining = toMutableList()
            while (remaining.isNotEmpty()) {
                var group = remaining.removeFirst()
                while (true) {
                    val (intersects, nonIntersects) = remaining.partition { it.intersect(group).isNotEmpty() }
                    if (intersects.isEmpty()) break
                    group += intersects.flatten().toSet()
                    remaining = nonIntersects.toMutableList()
                }
                result.add(group)
            }
            return result
        }
    }
}