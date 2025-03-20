package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.PropertySpec
import io.github.klahap.pgen.model.sql.Table


private fun Table.Column.getDefaultExpression(): Pair<String, List<Any>>? = when (type) {
    Table.Column.Type.Primitive.TIMESTAMP -> when (default) {
        "now()" -> ".defaultExpression(%T)" to listOf(Poet.defaultExpTimestamp)
        else -> null
    }

    Table.Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> when (default) {
        "now()" -> ".defaultExpression(%T)" to listOf(Poet.defaultExpTimestampZ)
        else -> null
    }

    Table.Column.Type.Primitive.UUID -> when (default) {
        "gen_random_uuid()" -> ".defaultExpression(%T(%S, %T()))" to
                listOf(Poet.customFunction, "gen_random_uuid", Poet.uuidColumnType)

        else -> null
    }

    Table.Column.Type.Primitive.BOOL -> when (default) {
        "false" -> ".default(false)" to emptyList()
        "true" -> ".default(true)" to emptyList()
        else -> null
    }

    else -> null
}

context(CodeGenContext)
fun PropertySpec.Builder.initializer(column: Table.Column, postfix: String, postArgs: List<Any>) {
    val columnName = column.name.value
    var postfix = postfix
    var postArgs = postArgs.toTypedArray()
    column.getDefaultExpression()?.let {
        postfix = it.first + postfix
        postArgs = (it.second + postArgs.toList()).toTypedArray()
    }

    when (val type = column.type) {
        is Table.Column.Type.NonPrimitive.Array -> {
            when (val elementType = type.elementType) {
                is Table.Column.Type.NonPrimitive.Enum -> initializer(
                    """
                    |%T<%T>(
                    |    name = %S,
                    |    sql = %S,
                    |    fromDb = { %T<%T>(it as String) },
                    |    toDb = { it.toPgObject() },
                    |)$postfix""".trimMargin(),
                    customEnumerationArray,
                    type.getTypeName(),
                    columnName,
                    "${elementType.name.schema.schemaName}.${elementType.name.name}",
                    typeNameGetPgEnumByLabel,
                    elementType.name.typeName,
                    *postArgs,
                )

                else -> initializer(
                    "array<%T>(name = %S)$postfix",
                    type.getTypeName(), columnName, *postArgs
                )
            }
        }

        is Table.Column.Type.NonPrimitive.Enum -> initializer(
            """
            customEnumeration(
                name = %S,
                sql = %S,
                fromDb = { %T<%T>(it as String) },
                toDb = { it.toPgObject() },
            )$postfix""".trimIndent(),
            columnName,
            "${type.name.schema.schemaName}.${type.name.name}",
            typeNameGetPgEnumByLabel,
            type.name.typeName,
            *postArgs
        )

        is Table.Column.Type.NonPrimitive.Domain -> initializer(
            """
            %T<%T>(
                name = %S,
                sqlType = %S,
            )$postfix""".trimIndent(),
            domainType,
            type.getDomainTypename(),
            columnName,
            type.sqlType,
            *postArgs
        )

        is Table.Column.Type.NonPrimitive.Reference -> initializer(
            """
            %T<%T>(
                name = %S,
                sqlType = %S,
            )$postfix""".trimIndent(),
            domainType,
            type.clazz.poet,
            columnName,
            type.sqlType,
            *postArgs
        )

        Table.Column.Type.Primitive.INT8 -> initializer("long(name = %S)$postfix", columnName, *postArgs)
        Table.Column.Type.Primitive.BOOL -> initializer("bool(name = %S)$postfix", columnName, *postArgs)
        Table.Column.Type.Primitive.BINARY -> initializer("binary(name = %S)$postfix", columnName, *postArgs)
        Table.Column.Type.Primitive.VARCHAR -> initializer("text(name = %S)$postfix", columnName, *postArgs)
        Table.Column.Type.Primitive.DATE -> initializer("%T(name = %S)$postfix", Poet.date, columnName, *postArgs)
        Table.Column.Type.Primitive.INTERVAL -> initializer("duration(name = %S)$postfix", columnName, *postArgs)
        Table.Column.Type.Primitive.INT4RANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, typeNameInt4RangeColumnType, *postArgs
        )

        Table.Column.Type.Primitive.INT8RANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, typeNameInt8RangeColumnType, *postArgs
        )

        Table.Column.Type.Primitive.INT4MULTIRANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, typeNameInt4MultiRangeColumnType, *postArgs
        )

        Table.Column.Type.Primitive.INT8MULTIRANGE -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, typeNameInt8MultiRangeColumnType, *postArgs
        )

        Table.Column.Type.Primitive.INT4 -> initializer("integer(name = %S)$postfix", columnName, *postArgs)
        Table.Column.Type.Primitive.JSON -> initializer(
            "%T<%T>(name = %S, serialize = %T)$postfix",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json, *postArgs
        )

        Table.Column.Type.Primitive.JSONB -> initializer(
            "%T<%T>(name = %S, jsonConfig = %T)$postfix",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json, *postArgs
        )

        is Table.Column.Type.NonPrimitive.Numeric -> initializer(
            "decimal(name = %S, precision = ${type.precision}, scale = ${type.scale})$postfix",
            columnName, *postArgs
        )

        Table.Column.Type.Primitive.INT2 -> initializer("short(name = %S)$postfix", columnName, *postArgs)
        Table.Column.Type.Primitive.TEXT -> initializer("text(name = %S)$postfix", columnName, *postArgs)
        Table.Column.Type.Primitive.TIME -> initializer("%T(name = %S)$postfix", Poet.time, columnName, *postArgs)
        Table.Column.Type.Primitive.TIMESTAMP -> initializer(
            "%T(name = %S)$postfix",
            Poet.timestamp,
            columnName,
            *postArgs
        )

        Table.Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> initializer(
            "%T(name = %S)$postfix",
            Poet.timestampWithTimeZone, columnName, *postArgs
        )

        Table.Column.Type.Primitive.UUID -> initializer("uuid(name = %S)$postfix", columnName, *postArgs)
        Table.Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, typeNameUnconstrainedNumericColumnType, *postArgs
        )
    }
}
