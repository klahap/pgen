package io.github.klahap.pgen.model.sql

import io.github.klahap.pgen.util.toCamelCase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Table(
    override val name: SqlObjectName,
    val columns: List<Column>,
    val primaryKey: PrimaryKey?,
    val foreignKeys: List<ForeignKey>,
) : SqlObject, Comparable<Table> {
    override fun compareTo(other: Table) = name.compareTo(other.name)

    @JvmInline
    @Serializable
    value class ColumnName(val value: String) {
        override fun toString() = value
        val pretty get() = value.toCamelCase(capitalized = false)
    }

    @Serializable
    data class Column(
        val name: ColumnName,
        val type: Type,
        val isNullable: Boolean,
    ) {
        val prettyName get() = name.pretty

        @Serializable
        sealed interface Type {
            @Serializable
            @SerialName("array")
            data class Array(val elementType: Type) : Type

            @Serializable
            @SerialName("enum")
            data class Enum(val name: SqlEnumName) : Type

            @Serializable
            @SerialName("int2")
            data object Int2 : Type

            @Serializable
            @SerialName("int4")
            data object Int4 : Type

            @Serializable
            @SerialName("int8")
            data object Int8 : Type

            @Serializable
            @SerialName("bool")
            data object Bool : Type

            @Serializable
            @SerialName("binary")
            data object Binary : Type

            @Serializable
            @SerialName("varchar")
            data object VarChar : Type

            @Serializable
            @SerialName("date")
            data object Date : Type

            @Serializable
            @SerialName("interval")
            data object Interval : Type

            @Serializable
            @SerialName("int4range")
            data object Int4Range : Type

            @Serializable
            @SerialName("int8range")
            data object Int8Range : Type

            @Serializable
            @SerialName("int4MultiRange")
            data object Int4MultiRange : Type

            @Serializable
            @SerialName("int8MultiRange")
            data object Int8MultiRange : Type

            @Serializable
            @SerialName("json")
            data object Json : Type

            @Serializable
            @SerialName("jsonb")
            data object Jsonb : Type

            @Serializable
            @SerialName("numeric")
            data class Numeric(val precision: Int, val scale: Int) : Type

            @Serializable
            @SerialName("unconstrainedNumeric")
            data object UnconstrainedNumeric : Type

            @Serializable
            @SerialName("text")
            data object Text : Type

            @Serializable
            @SerialName("time")
            data object Time : Type

            @Serializable
            @SerialName("timestamp")
            data object Timestamp : Type

            @Serializable
            @SerialName("timestampWithTimeZone")
            data object TimestampWithTimeZone : Type

            @Serializable
            @SerialName("uuid")
            data object Uuid : Type
        }
    }

    @Serializable
    data class PrimaryKey(val keyName: String, val columnNames: List<ColumnName>)

    @Serializable
    data class ForeignKey(
        val name: String,
        val targetTable: SqlTableName,
        val references: List<KeyPair>,
    ) {
        @Serializable
        data class KeyPair(
            val sourceColumn: ColumnName,
            val targetColumn: ColumnName,
        )

        fun toTyped() = if (references.size == 1)
            ForeignKeyTyped.SingleKey(
                name = name,
                targetTable = targetTable,
                reference = references.single(),
            )
        else
            ForeignKeyTyped.MultiKey(
                name = name,
                targetTable = targetTable,
                references = references,
            )
    }

    sealed interface ForeignKeyTyped {
        val name: String
        val targetTable: SqlTableName

        data class SingleKey(
            override val name: String,
            override val targetTable: SqlTableName,
            val reference: ForeignKey.KeyPair,
        ) : ForeignKeyTyped

        data class MultiKey(
            override val name: String,
            override val targetTable: SqlTableName,
            val references: List<ForeignKey.KeyPair>,
        ) : ForeignKeyTyped
    }
}
