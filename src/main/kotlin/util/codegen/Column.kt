package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.model.sql.Column


context(CodeGenContext)
private fun Column.getDefaultExpression(): Pair<String, List<Any>>? = when (type) {
    Column.Type.Primitive.TIMESTAMP -> when (default) {
        "now()" -> ".defaultExpression(%T)" to listOf(Poet.defaultExpTimestamp)
        else -> null
    }

    Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> when (default) {
        "now()" -> ".defaultExpression(%T)" to listOf(Poet.defaultExpTimestampZ)
        else -> null
    }

    Column.Type.Primitive.UUID -> when (default) {
        "gen_random_uuid()" -> ".defaultExpression(%T(%S, %T()))" to
                listOf(Poet.customFunction, "gen_random_uuid", Poet.uuidColumnType)

        else -> null
    }

    Column.Type.Primitive.BOOL -> when (default) {
        "false" -> ".default(false)" to emptyList()
        "true" -> ".default(true)" to emptyList()
        else -> null
    }

    else -> null
}

context(CodeGenContext)
fun PropertySpec.Builder.initializer(column: Column, postfix: String, postArgs: List<Any>) {
    val columnName = column.name.value
    var postfix = postfix
    var postArgs = postArgs.toTypedArray()
    column.getDefaultExpression()?.let {
        postfix = it.first + postfix
        postArgs = (it.second + postArgs.toList()).toTypedArray()
    }

    when (val type = column.type) {
        is Column.Type.NonPrimitive.Array -> {
            when (val elementType = type.elementType) {
                is Column.Type.NonPrimitive.Enum -> initializer(
                    """
                    |%T<%T>(
                    |    name = %S,
                    |    sql = %S,
                    |    fromDb = { %T<%T>(it as String) },
                    |    toDb = { it.toPgObject() },
                    |)$postfix""".trimMargin(),
                    poet.customEnumerationArray,
                    type.getTypeName(),
                    columnName,
                    "${elementType.name.schema.schemaName}.${elementType.name.name}",
                    poet.getPgEnumByLabel,
                    elementType.name.typeName,
                    *postArgs,
                )

                is Column.Type.NonPrimitive.Composite -> initializer(
                    "array<%T>(name = %S, columnType = %T)$postfix",
                    type.getTypeName(), columnName, elementType.getColumnTypeTypeName(), *postArgs
                )

                else -> initializer(
                    "array<%T>(name = %S)$postfix",
                    type.getTypeName(), columnName, *postArgs
                )
            }
        }

        is Column.Type.NonPrimitive.Enum -> initializer(
            """
            customEnumeration(
                name = %S,
                sql = %S,
                fromDb = { %T<%T>(it as String) },
                toDb = { it.toPgObject() },
            )$postfix""".trimIndent(),
            columnName,
            "${type.name.schema.schemaName}.${type.name.name}",
            poet.getPgEnumByLabel,
            type.name.typeName,
            *postArgs
        )

        is Column.Type.NonPrimitive.Composite -> initializer(
            "registerColumn(name = %S, type = %T)$postfix",
            columnName,
            type.getColumnTypeTypeName(),
            *postArgs,
        )

        is Column.Type.NonPrimitive.DomainType -> initializer(
            """
            %T<%T>(
                name = %S,
                sqlType = %S,
                builder = { %T${type.parserFunction}(it as %T) },
            )$postfix""".trimIndent(),
            poet.domainType,
            type.getDomainTypename(),
            columnName,
            type.sqlType,
            type.getDomainTypename(),
            type.originalType.getTypeName(),
            *postArgs
        )

        is Column.Type.NonPrimitive.PgVector -> initializer(
            """
            %T(
                name = %S,
                schema = %S,
            )$postfix""".trimIndent(),
            poet.packageCustomColumn.className("pgVector"),
            columnName,
            type.schema,
            *postArgs
        )

        Column.Type.Primitive.INT8 -> initializer("long(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.BOOL -> initializer("bool(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.BINARY -> initializer("binary(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.VARCHAR -> initializer("text(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.DATE -> initializer("%T(name = %S)$postfix", Poet.date, columnName, *postArgs)
        Column.Type.Primitive.INTERVAL -> initializer("duration(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.INT4RANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, poet.int4RangeColumnType, *postArgs
        )

        Column.Type.Primitive.INT8RANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, poet.int8RangeColumnType, *postArgs
        )

        Column.Type.Primitive.INT4MULTIRANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, poet.int4MultiRangeColumnType, *postArgs
        )

        Column.Type.Primitive.INT8MULTIRANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, poet.int8MultiRangeColumnType, *postArgs
        )

        Column.Type.Primitive.INT4 -> initializer("integer(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.FLOAT4 -> initializer("float(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.FLOAT8 -> initializer("double(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.JSON -> initializer(
            "%T<%T>(name = %S, serialize = %T)$postfix",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json, *postArgs
        )

        Column.Type.Primitive.JSONB -> initializer(
            "%T<%T>(name = %S, jsonConfig = %T)$postfix",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json, *postArgs
        )

        is Column.Type.NonPrimitive.Numeric -> initializer(
            "decimal(name = %S, precision = ${type.precision}, scale = ${type.scale})$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.INT2 -> initializer("short(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.TEXT -> initializer("text(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.TIME -> initializer("%T(name = %S)$postfix", Poet.time, columnName, *postArgs)
        Column.Type.Primitive.TIMESTAMP -> initializer(
            "%T(name = %S)$postfix",
            Poet.timestamp,
            columnName,
            *postArgs
        )

        Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> initializer(
            "%T(name = %S)$postfix",
            Poet.timestampWithTimeZone, columnName, *postArgs
        )

        Column.Type.Primitive.UUID -> initializer("uuid(name = %S)$postfix", columnName, *postArgs)
        Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, poet.unconstrainedNumericColumnType, *postArgs
        )
    }
}

context(CodeGenContext)
internal fun Column.getColumnTypeName() = when (type) {
    is Column.Type.NonPrimitive.Array -> List::class.asTypeName()
        .parameterizedBy(type.getTypeName())

    else -> type.getTypeName()
}.copy(nullable = isNullable)
