package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.model.sql.Table
import java.math.BigDecimal
import java.util.*


context(CodeGenContext)
fun Table.Column.Type.getTypeName(): TypeName {
    return when (this) {
        is Table.Column.Type.NonPrimitive.Array -> elementType.getTypeName()
        is Table.Column.Type.NonPrimitive.Enum -> name.typeName
        is Table.Column.Type.NonPrimitive.Numeric -> BigDecimal::class.asTypeName()
        Table.Column.Type.Primitive.INT8 -> Long::class.asTypeName()
        Table.Column.Type.Primitive.BOOL -> Boolean::class.asTypeName()
        Table.Column.Type.Primitive.BINARY -> ByteArray::class.asTypeName()
        Table.Column.Type.Primitive.VARCHAR -> String::class.asTypeName()
        Table.Column.Type.Primitive.DATE -> Poet.localDate
        Table.Column.Type.Primitive.INTERVAL -> Poet.duration
        Table.Column.Type.Primitive.INT4RANGE -> IntRange::class.asTypeName()
        Table.Column.Type.Primitive.INT8RANGE -> LongRange::class.asTypeName()
        Table.Column.Type.Primitive.INT4MULTIRANGE -> typeNameMultiRange.parameterizedBy(Int::class.asTypeName())
        Table.Column.Type.Primitive.INT8MULTIRANGE -> typeNameMultiRange.parameterizedBy(Long::class.asTypeName())
        Table.Column.Type.Primitive.INT4 -> Int::class.asTypeName()
        Table.Column.Type.Primitive.JSON -> Poet.jsonElement
        Table.Column.Type.Primitive.JSONB -> Poet.jsonElement
        Table.Column.Type.Primitive.INT2 -> Short::class.asTypeName()
        Table.Column.Type.Primitive.TEXT -> String::class.asTypeName()
        Table.Column.Type.Primitive.TIME -> Poet.localTime
        Table.Column.Type.Primitive.TIMESTAMP -> Poet.instant
        Table.Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> Poet.offsetDateTime
        Table.Column.Type.Primitive.UUID -> UUID::class.asTypeName()
        Table.Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> BigDecimal::class.asTypeName()
    }
}

context(CodeGenContext)
fun PropertySpec.Builder.initializer(column: Table.Column, postFix: String, vararg postArgs: Any) {
    val columnName = column.name.value
    when (val type = column.type) {
        is Table.Column.Type.NonPrimitive.Array -> initializer(
            "array<%T>(name = %S)$postFix",
            type.getTypeName(), columnName, *postArgs
        )

        is Table.Column.Type.NonPrimitive.Enum -> initializer(
            """
            customEnumeration(
                name = %S,
                sql = %S,
                fromDb = { %T<%T>(it as String) },
                toDb = { it.toPgObject() },
            )$postFix""".trimIndent(),
            columnName,
            type.name.name,
            typeNameGetPgEnumByLabel,
            type.name.typeName,
            *postArgs
        )

        Table.Column.Type.Primitive.INT8 -> initializer("long(name = %S)$postFix", columnName, *postArgs)
        Table.Column.Type.Primitive.BOOL -> initializer("bool(name = %S)$postFix", columnName, *postArgs)
        Table.Column.Type.Primitive.BINARY -> initializer("binary(name = %S)$postFix", columnName, *postArgs)
        Table.Column.Type.Primitive.VARCHAR -> initializer("text(name = %S)$postFix", columnName, *postArgs)
        Table.Column.Type.Primitive.DATE -> initializer("%T(name = %S)$postFix", Poet.date, columnName, *postArgs)
        Table.Column.Type.Primitive.INTERVAL -> initializer("duration(name = %S)$postFix", columnName, *postArgs)
        Table.Column.Type.Primitive.INT4RANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postFix",
            columnName, typeNameInt4RangeColumnType, *postArgs
        )

        Table.Column.Type.Primitive.INT8RANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postFix",
            columnName, typeNameInt8RangeColumnType, *postArgs
        )

        Table.Column.Type.Primitive.INT4MULTIRANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postFix",
            columnName, typeNameInt4MultiRangeColumnType, *postArgs
        )

        Table.Column.Type.Primitive.INT8MULTIRANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postFix",
            columnName, typeNameInt8MultiRangeColumnType, *postArgs
        )

        Table.Column.Type.Primitive.INT4 -> initializer("integer(name = %S)$postFix", columnName, *postArgs)
        Table.Column.Type.Primitive.JSON -> initializer(
            "%T<%T>(name = %S, serialize = %T)$postFix",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json, *postArgs
        )

        Table.Column.Type.Primitive.JSONB -> initializer(
            "%T<%T>(name = %S, jsonConfig = %T)$postFix",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json, *postArgs
        )

        is Table.Column.Type.NonPrimitive.Numeric -> initializer(
            "decimal(name = %S, precision = ${type.precision}, scale = ${type.scale})$postFix",
            columnName, *postArgs
        )

        Table.Column.Type.Primitive.INT2 -> initializer("short(name = %S)$postFix", columnName, *postArgs)
        Table.Column.Type.Primitive.TEXT -> initializer("text(name = %S)$postFix", columnName, *postArgs)
        Table.Column.Type.Primitive.TIME -> initializer("%T(name = %S)$postFix", Poet.time, columnName, *postArgs)
        Table.Column.Type.Primitive.TIMESTAMP -> initializer("%T(name = %S)$postFix", Poet.timestamp, columnName, *postArgs)
        Table.Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> initializer(
            "%T(name = %S)$postFix",
            Poet.timestampWithTimeZone, columnName, *postArgs
        )

        Table.Column.Type.Primitive.UUID -> initializer("uuid(name = %S)$postFix", columnName, *postArgs)
        Table.Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> initializer(
            "registerColumn(name = %S, type = %T())$postFix",
            columnName, typeNameUnconstrainedNumericColumnType, *postArgs
        )
    }
}
