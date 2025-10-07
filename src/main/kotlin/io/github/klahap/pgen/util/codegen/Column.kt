package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.model.sql.Column


context(_: CodeGenContext)
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

context(c: CodeGenContext)
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
                is Column.Type.NonPrimitive.Enum -> @Suppress("SpreadOperator") initializer(
                    """
                    |%T<%T>(
                    |    name = %S,
                    |    sql = %S,
                    |    fromDb = { %T<%T>(it as String) },
                    |    toDb = { it.%T() },
                    |)$postfix""".trimMargin(),
                    c.poet.customEnumerationArray,
                    type.getTypeName(),
                    columnName,
                    "${elementType.name.schema.schemaName}.${elementType.name.name}",
                    c.poet.getPgEnumByLabel,
                    elementType.name.typeName,
                    c.poet.toDbObject,
                    *postArgs,
                )

                is Column.Type.NonPrimitive.Composite -> @Suppress("SpreadOperator") initializer(
                    "array<%T>(name = %S, columnType = %T)$postfix",
                    type.getTypeName(), columnName, elementType.getColumnTypeTypeName(), *postArgs
                )

                else -> @Suppress("SpreadOperator") initializer(
                    "array<%T>(name = %S)$postfix",
                    type.getTypeName(), columnName, *postArgs
                )
            }
        }

        is Column.Type.NonPrimitive.Enum -> @Suppress("SpreadOperator") initializer(
            """
            customEnumeration(
                name = %S,
                sql = %S,
                fromDb = { %T<%T>(it as String) },
                toDb = { it.%T() },
            )$postfix""".trimIndent(),
            columnName,
            "${type.name.schema.schemaName}.${type.name.name}",
            c.poet.getPgEnumByLabel,
            type.name.typeName,
            c.poet.toDbObject,
            *postArgs
        )

        is Column.Type.NonPrimitive.Composite -> @Suppress("SpreadOperator") initializer(
            "registerColumn(name = %S, type = %T)$postfix",
            columnName, type.getColumnTypeTypeName(), *postArgs,
        )

        is Column.Type.NonPrimitive.DomainType -> @Suppress("SpreadOperator") initializer(
            """
            %T<%T>(
                name = %S,
                sqlType = %S,
                builder = { %T${type.parserFunction}(it as %T) },
            )$postfix""".trimIndent(),
            c.poet.domainType,
            type.getDomainTypename(),
            columnName,
            type.sqlType,
            type.getDomainTypename(),
            type.originalType.getTypeName(),
            *postArgs
        )

        is Column.Type.NonPrimitive.PgVector -> @Suppress("SpreadOperator") initializer(
            """
            %T(
                name = %S,
                schema = %S,
            )$postfix""".trimIndent(),
            c.poet.packageCustomColumn.className("pgVector"),
            columnName,
            type.schema,
            *postArgs
        )

        Column.Type.Primitive.INT8 -> @Suppress("SpreadOperator") initializer(
            "long(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.BOOL -> @Suppress("SpreadOperator") initializer(
            "bool(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.BINARY -> @Suppress("SpreadOperator") initializer(
            "binary(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.VARCHAR -> @Suppress("SpreadOperator") initializer(
            "text(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.DATE -> @Suppress("SpreadOperator") initializer(
            "%T(name = %S)$postfix",
            Poet.date, columnName, *postArgs
        )

        Column.Type.Primitive.INTERVAL -> @Suppress("SpreadOperator") initializer(
            "duration(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.INT4RANGE -> @Suppress("SpreadOperator") initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, c.poet.int4RangeColumnType, *postArgs
        )

        Column.Type.Primitive.INT8RANGE -> @Suppress("SpreadOperator") initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, c.poet.int8RangeColumnType, *postArgs
        )

        Column.Type.Primitive.INT4MULTIRANGE -> @Suppress("SpreadOperator") initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, c.poet.int4MultiRangeColumnType, *postArgs
        )

        Column.Type.Primitive.INT8MULTIRANGE -> @Suppress("SpreadOperator") initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, c.poet.int8MultiRangeColumnType, *postArgs
        )

        Column.Type.Primitive.INT4 -> @Suppress("SpreadOperator") initializer(
            "integer(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.FLOAT4 -> @Suppress("SpreadOperator") initializer(
            "float(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.FLOAT8 -> @Suppress("SpreadOperator") initializer(
            "double(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.JSON -> @Suppress("SpreadOperator") initializer(
            "%T<%T>(name = %S, serialize = %T)$postfix",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json, *postArgs
        )

        Column.Type.Primitive.JSONB -> @Suppress("SpreadOperator") initializer(
            "%T<%T>(name = %S, jsonConfig = %T)$postfix",
            Poet.jsonColumn, Poet.jsonElement, columnName, Poet.json, *postArgs
        )

        is Column.Type.NonPrimitive.Numeric -> @Suppress("SpreadOperator") initializer(
            "decimal(name = %S, precision = ${type.precision}, scale = ${type.scale})$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.INT2 -> @Suppress("SpreadOperator") initializer(
            "short(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.TEXT -> @Suppress("SpreadOperator") initializer(
            "text(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.TIME -> @Suppress("SpreadOperator") initializer(
            "%T(name = %S)$postfix",
            Poet.time, columnName, *postArgs
        )

        Column.Type.Primitive.TIMESTAMP -> @Suppress("SpreadOperator") initializer(
            "%T(name = %S)$postfix",
            Poet.timestamp, columnName, *postArgs
        )

        Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> @Suppress("SpreadOperator") initializer(
            "%T(name = %S)$postfix",
            Poet.timestampWithTimeZone, columnName, *postArgs
        )

        Column.Type.Primitive.UUID -> @Suppress("SpreadOperator") initializer(
            "uuid(name = %S)$postfix",
            columnName, *postArgs
        )

        Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> @Suppress("SpreadOperator") initializer(
            "registerColumn(name = %S, type = %T())$postfix",
            columnName, c.poet.unconstrainedNumericColumnType, *postArgs
        )

        Column.Type.Primitive.REG_CLASS -> @Suppress("SpreadOperator") initializer(
            "%T(name = %S)$postfix".trimIndent(),
            c.poet.regClassColumn,
            columnName,
            *postArgs
        )
    }
}

context(c: CodeGenContext)
internal fun Column.getColumnTypeName() = when (type) {
    is Column.Type.NonPrimitive.Array -> List::class.asTypeName()
        .parameterizedBy(type.getTypeName())

    else -> type.getTypeName()
}.copy(nullable = isNullable)
