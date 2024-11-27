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
        is Table.Column.Type.Array -> elementType.getTypeName()
        is Table.Column.Type.Enum -> name.typeName
        Table.Column.Type.Int8 -> Long::class.asTypeName()
        Table.Column.Type.Bool -> Boolean::class.asTypeName()
        Table.Column.Type.VarChar -> String::class.asTypeName()
        Table.Column.Type.Date -> Poet.localDate
        Table.Column.Type.Interval -> Poet.duration
        Table.Column.Type.Int4Range -> IntRange::class.asTypeName()
        Table.Column.Type.Int8Range -> LongRange::class.asTypeName()
        Table.Column.Type.Int4MultiRange -> Poet.multiRange.parameterizedBy(Int::class.asTypeName())
        Table.Column.Type.Int8MultiRange -> Poet.multiRange.parameterizedBy(Long::class.asTypeName())
        Table.Column.Type.Int4 -> Int::class.asTypeName()
        Table.Column.Type.Json -> Poet.jsonElement
        Table.Column.Type.Jsonb -> Poet.jsonElement
        is Table.Column.Type.Numeric -> BigDecimal::class.asTypeName()
        Table.Column.Type.Int2 -> Short::class.asTypeName()
        Table.Column.Type.Text -> String::class.asTypeName()
        Table.Column.Type.Time -> Poet.localTime
        Table.Column.Type.Timestamp -> Poet.instant
        Table.Column.Type.TimestampWithTimeZone -> Poet.offsetDateTime
        Table.Column.Type.Uuid -> UUID::class.asTypeName()
        Table.Column.Type.UnconstrainedNumeric -> BigDecimal::class.asTypeName()
    }
}

context(CodeGenContext)
fun PropertySpec.Builder.initializer(column: Table.Column) {
    val columnName = column.name.value
    when (val type = column.type) {
        is Table.Column.Type.Array -> initializer("array<%T>(name = %S)", type.getTypeName(), columnName)
        is Table.Column.Type.Enum -> initializer(
            """
            customEnumeration(
                name = %S,
                sql = %S,
                fromDb = { %T(it as String) },
                toDb = { it.toPgObject() },
            )""".trimIndent(), columnName, type.name.name, Poet.getPgEnumByLabel
        )

        Table.Column.Type.Int8 -> initializer("long(name = %S)", columnName)
        Table.Column.Type.Bool -> initializer("bool(name = %S)", columnName)
        Table.Column.Type.VarChar -> initializer("text(name = %S)", columnName)
        Table.Column.Type.Date -> initializer("%T(name = %S)", Poet.date, columnName)
        Table.Column.Type.Interval -> initializer("duration(name = %S)", columnName)
        Table.Column.Type.Int4Range -> initializer(
            "registerColumn(name = %S, type = %T())",
            columnName, Poet.int4RangeColumnType
        )

        Table.Column.Type.Int8Range -> initializer(
            "registerColumn(name = %S, type = %T())",
            columnName, Poet.int8RangeColumnType
        )

        Table.Column.Type.Int4MultiRange -> initializer(
            "registerColumn(name = %S, type = %T())",
            columnName, Poet.int4MultiRangeColumnType
        )

        Table.Column.Type.Int8MultiRange -> initializer(
            "registerColumn(name = %S, type = %T())",
            columnName, Poet.int8MultiRangeColumnType
        )

        Table.Column.Type.Int4 -> initializer("integer(name = %S)", columnName)
        Table.Column.Type.Json -> initializer(
            "%T<%T>(name = %S, serialize = %T)",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json,
        )

        Table.Column.Type.Jsonb -> initializer(
            "%T<%T>(name = %S, jsonConfig = %T)",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json,
        )

        is Table.Column.Type.Numeric -> initializer(
            "decimal(name = %S, precision = ${type.precision}, scale = ${type.scale})",
            columnName,
        )

        Table.Column.Type.Int2 -> initializer("short(name = %S)", columnName)
        Table.Column.Type.Text -> initializer("text(name = %S)", columnName)
        Table.Column.Type.Time -> initializer("%T(name = %S)", Poet.time, columnName)
        Table.Column.Type.Timestamp -> initializer("%T(name = %S)", Poet.timestamp, columnName)
        Table.Column.Type.TimestampWithTimeZone -> initializer("%T(name = %S)", Poet.timestampWithTimeZone, columnName)
        Table.Column.Type.Uuid -> initializer("uuid(name = %S)", columnName)
        Table.Column.Type.UnconstrainedNumeric -> initializer(
            "registerColumn(name = %S, type = %T())",
            columnName, Poet.unconstrainedNumericColumnType
        )
    }
}
