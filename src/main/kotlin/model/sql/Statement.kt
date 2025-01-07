package io.github.klahap.pgen.model.sql

import io.github.klahap.pgen.util.kotlinKeywords
import io.github.klahap.pgen.util.makeDifferent
import io.github.klahap.pgen.util.toCamelCase
import kotlinx.serialization.Serializable


@Serializable
data class Statement(
    val name: SqlStatementName,
    val cardinality: Cardinality,
    val variables: List<VariableName>,
    val variableTypes: Map<VariableName, Table.Column.Type>,
    val columns: List<Table.Column>,
    val sql: String,
) {
    @JvmInline
    @Serializable
    value class VariableName(val name: String) : Comparable<VariableName> {
        val pretty get() = name.toCamelCase(capitalized = false)
            .makeDifferent(kotlinKeywords  + setOf("coroutineContext", "db"))
        override fun compareTo(other: VariableName): Int = name.compareTo(other.name)
    }

    data class Raw(
        val name: String,
        val cardinality: Cardinality,
        val allVariables: List<VariableName>,
        val uniqueSortedVariables: List<VariableName>,
        val nonNullColumns: Set<String>,
        val sql: String,
        val preparedPsql: String,
        val preparedSql: String,
    )

    @Serializable
    enum class Cardinality {
        ONE,
        MANY,
    }
}
