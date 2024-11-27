package io.github.klahap.pgen.model.sql

import io.github.klahap.pgen.util.toCamelCase

data class Table(
    override val name: SqlObjectName,
    val columns: List<Column>,
    val primaryKey: PrimaryKey?,
    val foreignKeys: List<ForeignKey>,
) : SqlObject {
    @JvmInline
    value class ColumnName(val value: String) {
        override fun toString() = value
        val pretty get() = value.toCamelCase(capitalized = false)
    }

    data class Column(
        val name: ColumnName,
        val type: Type,
        val isNullable: Boolean,
    ) {
        val prettyName get() = name.pretty

        sealed interface Type {
            data class Array(val elementType: Type) : Type
            data class Enum(val name: SqlEnumName) : Type
            data object Int2 : Type
            data object Int4 : Type
            data object Int8 : Type
            data object Bool : Type
            data object VarChar : Type
            data object Date : Type
            data object Interval : Type
            data object Int4Range : Type
            data object Int8Range : Type
            data object Int4MultiRange : Type
            data object Int8MultiRange : Type
            data object Json : Type
            data object Jsonb : Type
            data class Numeric(val precision: Int, val scale: Int) : Type
            data object UnconstrainedNumeric : Type
            data object Text : Type
            data object Time : Type
            data object Timestamp : Type
            data object TimestampWithTimeZone : Type
            data object Uuid : Type
        }
    }

    data class PrimaryKey(val keyName: String, val columnNames: List<ColumnName>)

    sealed interface ForeignKey {
        val name: String
        val targetTable: SqlTableName

        data class KeyPair(
            val sourceColumn: ColumnName,
            val targetColumn: ColumnName,
        )

        data class SingleKey(
            override val name: String,
            override val targetTable: SqlTableName,
            val reference: KeyPair,
        ) : ForeignKey

        data class MultiKey(
            override val name: String,
            override val targetTable: SqlTableName,
            val references: Set<KeyPair>,
        ) : ForeignKey
    }
}
