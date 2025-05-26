package io.github.klahap.pgen.model.sql

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import io.github.klahap.pgen.util.ColumnTypeSerializer
import io.github.klahap.pgen.util.codegen.CodeGenContext
import io.github.klahap.pgen.util.makeDifferent
import io.github.klahap.pgen.util.toCamelCase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class Column(
    @Transient val pos: Int = -1,
    val name: Name,
    val type: Type,
    val isNullable: Boolean = false,
    val default: String? = null,
) {
    val prettyName get() = name.pretty


    @JvmInline
    @Serializable
    value class Name(val value: String) {
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

    @Serializable(with = ColumnTypeSerializer::class)
    sealed interface Type {
        val sqlType: String

        @Serializable
        sealed interface NonPrimitive : Type {
            @Serializable
            @SerialName("array")
            data class Array(val elementType: Type) : NonPrimitive {
                override val sqlType get() = "${elementType.sqlType}[]"
            }

            @Serializable
            @SerialName("enum")
            data class Enum(val name: SqlObjectName) : NonPrimitive {
                override val sqlType get() = "${name.schema.schemaName}.${name.name}"
            }

            @Serializable
            @SerialName("composite")
            data class Composite(val name: SqlObjectName) : NonPrimitive {
                override val sqlType get() = "${name.schema.schemaName}.${name.name}"

                context(CodeGenContext)
                fun getColumnTypeTypeName() = ClassName("${name.packageName}", "${name.prettyName}.ColumnType")
            }

            @Serializable
            @SerialName("numeric")
            data class Numeric(val precision: Int, val scale: Int) : NonPrimitive {
                override val sqlType get() = ""
            }

            sealed interface DomainType : NonPrimitive {
                context(CodeGenContext)
                fun getDomainTypename(): TypeName

                context(CodeGenContext)
                fun getValueClass(): KotlinValueClass

                val originalType: Type

                context(CodeGenContext)
                val parserFunction: String
                    get() = getValueClass().parseFunction?.let { ".$it" } ?: ""
            }

            @Serializable
            @SerialName("domain")
            data class Domain(
                override val name: SqlObjectName,
                override val originalType: Type
            ) : SqlObject, DomainType {
                override val sqlType get() = "${name.schema.schemaName}.${name.name}"

                context(CodeGenContext)
                override fun getDomainTypename(): TypeName = typeMappings[name]?.name?.poet ?: name.typeName

                context(CodeGenContext)
                override fun getValueClass(): KotlinValueClass = typeMappings[name] ?: KotlinValueClass(
                    name = KotlinClassName(
                        packageName = name.packageName.name,
                        className = name.prettyName
                    ),
                    parseFunction = null,
                )
            }

            @Serializable
            @SerialName("reference")
            data class Reference(
                private val valueClass: KotlinValueClass,
                override val originalType: Type,
            ) : DomainType {
                override val sqlType: String get() = originalType.sqlType
                context(CodeGenContext)
                override fun getDomainTypename(): TypeName = valueClass.name.poet

                context(CodeGenContext)
                override fun getValueClass(): KotlinValueClass = valueClass
            }
        }

        @Serializable
        enum class Primitive(override val sqlType: String) : Type {
            BOOL("bool"),
            BINARY("bytea"),
            DATE("date"),
            INT2("int2"),
            INT4("int4"),
            INT8("int8"),
            FLOAT4("float4"),
            FLOAT8("float8"),
            INT4RANGE("int4range"),
            INT8RANGE("int8range"),
            INT4MULTIRANGE("int4multirange"),
            INT8MULTIRANGE("int8multirange"),
            INTERVAL("interval"),
            JSON("json"),
            JSONB("jsonb"),
            TEXT("text"),
            TIME("time"),
            TIMESTAMP("timestamp"),
            TIMESTAMP_WITH_TIMEZONE("timestamptz"),
            UUID("uuid"),
            VARCHAR("varchar"),
            UNCONSTRAINED_NUMERIC("numeric"),
        }
    }
}