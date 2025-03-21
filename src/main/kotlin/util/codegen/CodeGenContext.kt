package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.model.sql.KotlinValueClass
import io.github.klahap.pgen.model.sql.SqlColumnName
import io.github.klahap.pgen.model.sql.SqlObjectName
import io.github.klahap.pgen.model.sql.Table

class CodeGenContext(
    val rootPackageName: PackageName,
    val createDirectoriesForRootPackageName: Boolean,
    val typeMappings: Map<SqlObjectName, KotlinValueClass>,
    typeOverwrites: Map<SqlColumnName, KotlinValueClass>,
    typeGroups: List<Set<SqlColumnName>>,
) {
    val allTypeOverwrites: Map<SqlColumnName, KotlinValueClass>
    private val packageCustomColumn = PackageName("$rootPackageName.column_type")

    init {
        allTypeOverwrites = typeOverwrites.entries.flatMap { (column, clazz) ->
            val group = typeGroups.firstOrNull { it.contains(column) } ?: setOf(column)
            group.map { c -> c to clazz }
        }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
            .mapValues {
                it.value.singleOrNull() ?: throw Exception("multiple type overwrites for ${it.key}: ${it.value}")
            }
    }

    fun Table.update(): Table {
        val newColumns = columns.map { column ->
            val columnName = SqlColumnName(tableName = name, name = column.name.value)
            if (column.type is Table.Column.Type.NonPrimitive.Reference) return@map column
            val kotlinClass = allTypeOverwrites[columnName] ?: return@map column
            val newType = Table.Column.Type.NonPrimitive.Reference(
                valueClass = kotlinClass,
                originalType = when(val t = column.type) {
                    is Table.Column.Type.NonPrimitive.Domain -> t.originalType
                    else -> t
                },
            )
            column.copy(type = newType)
        }
        return copy(columns = newColumns)
    }

    val getArrayColumnType
        get() = ClassName(packageCustomColumn.name, "getArrayColumnType")
    val domainType
        get() = ClassName(packageCustomColumn.name, "domainType")
    val domainTypeColumn
        get() = ClassName(packageCustomColumn.name, "DomainTypeColumn")
    val customEnumerationArray
        get() = ClassName(packageCustomColumn.name, "customEnumerationArray")
    val typeNameMultiRange
        get() = ClassName(packageCustomColumn.name, "MultiRange")
    val defaultJsonColumnType
        get() = ClassName(packageCustomColumn.name, "DefaultJsonColumnType")
    val typeNameInt4RangeColumnType
        get() = ClassName(packageCustomColumn.name, "Int4RangeColumnType")
    val typeNameInt8RangeColumnType
        get() = ClassName(packageCustomColumn.name, "Int8RangeColumnType")
    val typeNameInt4MultiRangeColumnType
        get() = ClassName(packageCustomColumn.name, "Int4MultiRangeColumnType")
    val typeNameInt8MultiRangeColumnType
        get() = ClassName(packageCustomColumn.name, "Int8MultiRangeColumnType")
    val typeNameUnconstrainedNumericColumnType
        get() = ClassName(packageCustomColumn.name, "UnconstrainedNumericColumnType")

    val typeNamePgEnum get() = ClassName(packageCustomColumn.name, "PgEnum")
    val typeNameGetPgEnumByLabel get() = ClassName(packageCustomColumn.name, "getPgEnumByLabel")

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