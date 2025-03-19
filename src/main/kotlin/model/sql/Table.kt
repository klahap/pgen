package io.github.klahap.pgen.model.sql

import com.squareup.kotlinpoet.TypeName
import io.github.klahap.pgen.util.ColumnTypeSerializer
import io.github.klahap.pgen.util.codegen.CodeGenContext
import io.github.klahap.pgen.util.makeDifferent
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
        val pretty get() = value.toCamelCase(capitalized = false).makeDifferent(reservedColumnNames)

        companion object {
            private val reservedColumnNames = setOf(
                "tableName",
                "schemaName",
                "tableNameWithoutScheme",
                "tableNameWithoutSchemeSanitized",
                "_columns",
                "columns",
                "autoIncColumn",
                "_indices",
                "indices",
                "_foreignKeys",
                "foreignKeys",
                "sequences",
                "checkConstraints",
                "generatedUnsignedCheckPrefix",
                "generatedSignedCheckPrefix",
                "primaryKey",
                "ddl",
                "source",
                "fields",
            )
        }
    }

    @Serializable
    data class Column(
        val name: ColumnName,
        val type: Type,
        val isNullable: Boolean = false,
        val default: String? = null,
    ) {
        val prettyName get() = name.pretty

        @Serializable(with = ColumnTypeSerializer::class)
        sealed interface Type {
            @Serializable
            sealed interface NonPrimitive : Type {
                @Serializable
                @SerialName("array")
                data class Array(val elementType: Type) : NonPrimitive

                @Serializable
                @SerialName("enum")
                data class Enum(val name: SqlObjectName) : NonPrimitive

                @Serializable
                @SerialName("numeric")
                data class Numeric(val precision: Int, val scale: Int) : NonPrimitive

                @Serializable
                @SerialName("domain")
                data class Domain(
                    override val name: SqlObjectName,
                    val originalType: Type
                ) : SqlObject, NonPrimitive {
                    val sqlType get() = "${name.schema.schemaName}.${name.name}"

                    context(CodeGenContext)
                    fun getDomainTypename(): TypeName = typeMappings[name] ?: name.typeName
                }
            }

            @Serializable
            enum class Primitive : Type {
                INT2,
                INT4,
                INT8,
                BOOL,
                BINARY,
                UNCONSTRAINED_NUMERIC,
                TEXT,
                TIME,
                TIMESTAMP,
                TIMESTAMP_WITH_TIMEZONE,
                UUID,
                VARCHAR,
                DATE,
                INTERVAL,
                INT4RANGE,
                INT8RANGE,
                INT4MULTIRANGE,
                INT8MULTIRANGE,
                JSON,
                JSONB,
            }
        }
    }

    @Serializable
    data class PrimaryKey(val keyName: String, val columnNames: List<ColumnName>)

    @Serializable
    data class ForeignKey(
        val name: String,
        val targetTable: SqlObjectName,
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
        val targetTable: SqlObjectName

        data class SingleKey(
            override val name: String,
            override val targetTable: SqlObjectName,
            val reference: ForeignKey.KeyPair,
        ) : ForeignKeyTyped

        data class MultiKey(
            override val name: String,
            override val targetTable: SqlObjectName,
            val references: List<ForeignKey.KeyPair>,
        ) : ForeignKeyTyped
    }
}
