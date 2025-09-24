package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.model.sql.Column
import java.math.BigDecimal
import java.util.UUID


context(c: CodeGenContext)
fun Column.Type.getTypeName(innerArrayType: Boolean = true): TypeName = when (this) {
    is Column.Type.NonPrimitive.Array -> if (innerArrayType)
        elementType.getTypeName()
    else
        List::class.asTypeName().parameterizedBy(elementType.getTypeName())

    is Column.Type.NonPrimitive.Domain -> getDomainTypename()
    is Column.Type.NonPrimitive.Reference -> getValueClass().name.poet
    is Column.Type.NonPrimitive.Enum -> name.typeName
    is Column.Type.NonPrimitive.PgVector -> FloatArray::class.asTypeName()
    is Column.Type.NonPrimitive.Composite -> name.typeName
    is Column.Type.NonPrimitive.Numeric -> BigDecimal::class.asTypeName()
    Column.Type.Primitive.INT8 -> Long::class.asTypeName()
    Column.Type.Primitive.BOOL -> Boolean::class.asTypeName()
    Column.Type.Primitive.BINARY -> ByteArray::class.asTypeName()
    Column.Type.Primitive.VARCHAR -> String::class.asTypeName()
    Column.Type.Primitive.DATE -> Poet.localDate
    Column.Type.Primitive.INTERVAL -> Poet.duration
    Column.Type.Primitive.INT4RANGE -> IntRange::class.asTypeName()
    Column.Type.Primitive.INT8RANGE -> LongRange::class.asTypeName()
    Column.Type.Primitive.INT4MULTIRANGE -> c.poet.multiRange.parameterizedBy(Int::class.asTypeName())
    Column.Type.Primitive.INT8MULTIRANGE -> c.poet.multiRange.parameterizedBy(Long::class.asTypeName())
    Column.Type.Primitive.INT4 -> Int::class.asTypeName()
    Column.Type.Primitive.FLOAT4 -> Float::class.asTypeName()
    Column.Type.Primitive.FLOAT8 -> Double::class.asTypeName()
    Column.Type.Primitive.JSON -> Poet.jsonElement
    Column.Type.Primitive.JSONB -> Poet.jsonElement
    Column.Type.Primitive.INT2 -> Short::class.asTypeName()
    Column.Type.Primitive.TEXT -> String::class.asTypeName()
    Column.Type.Primitive.TIME -> Poet.localTime
    Column.Type.Primitive.TIMESTAMP -> c.poet.instant
    Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> Poet.offsetDateTime
    Column.Type.Primitive.UUID -> UUID::class.asTypeName()
    Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> BigDecimal::class.asTypeName()
}

private fun codeBlock(format: String, vararg args: Any) = CodeBlock.builder().add(format, *args).build()

context(c: CodeGenContext)
fun Column.Type.getExposedColumnType(): CodeBlock = when (this) {
    is Column.Type.NonPrimitive.Array ->
        codeBlock("%T(%L)", c.poet.getArrayColumnType, elementType.getExposedColumnType())

    is Column.Type.NonPrimitive.PgVector ->
        codeBlock("%T(schema=%S)", c.poet.packageCustomColumn.className("PgVectorColumnType"), schema)

    is Column.Type.NonPrimitive.Composite ->
        codeBlock("%T(sqlType=%S)", getColumnTypeTypeName(), sqlType)

    is Column.Type.NonPrimitive.Enum ->
        codeBlock("%T(%T::class)", Poet.enumerationColumnType, name.typeName)

    is Column.Type.NonPrimitive.Numeric ->
        codeBlock("%T(precision = $precision, scale = $scale)", Poet.decimalColumnType)

    is Column.Type.NonPrimitive.Domain ->
        codeBlock("%T(kClass=%T::class, sqlType=%S)", c.poet.domainTypeColumn, getDomainTypename(), sqlType)

    is Column.Type.NonPrimitive.Reference ->
        codeBlock(
            "%T(kClass=%T::class, sqlType=%S)",
            c.poet.domainTypeColumn,
            getValueClass().name.poet,
            originalType.sqlType
        )

    Column.Type.Primitive.INT8 -> codeBlock("%T()", Poet.longColumnType)
    Column.Type.Primitive.BOOL -> codeBlock("%T()", Poet.booleanColumnType)
    Column.Type.Primitive.BINARY -> codeBlock("%T()", Poet.binaryColumnType)
    Column.Type.Primitive.VARCHAR -> codeBlock("%T()", Poet.textColumnType)
    Column.Type.Primitive.DATE -> codeBlock("%T()", Poet.kotlinLocalDateColumnType)
    Column.Type.Primitive.INTERVAL -> codeBlock("%T()", Poet.kotlinDurationColumnType)
    Column.Type.Primitive.INT4RANGE -> codeBlock("%T()", c.poet.int4RangeColumnType)
    Column.Type.Primitive.INT8RANGE -> codeBlock("%T()", c.poet.int8RangeColumnType)
    Column.Type.Primitive.INT4MULTIRANGE -> codeBlock("%T()", c.poet.int4MultiRangeColumnType)
    Column.Type.Primitive.INT8MULTIRANGE -> codeBlock("%T()", c.poet.int8MultiRangeColumnType)
    Column.Type.Primitive.INT4 -> codeBlock("%T()", Poet.integerColumnType)
    Column.Type.Primitive.FLOAT4 -> codeBlock("%T()", Poet.floatColumnType)
    Column.Type.Primitive.FLOAT8 -> codeBlock("%T()", Poet.doubleColumnType)
    Column.Type.Primitive.INT2 -> codeBlock("%T()", Poet.shortColumnType)
    Column.Type.Primitive.TEXT -> codeBlock("%T()", Poet.textColumnType)
    Column.Type.Primitive.TIME -> codeBlock("%T()", Poet.kotlinLocalTimeColumnType)
    Column.Type.Primitive.TIMESTAMP -> codeBlock("%T()", Poet.kotlinInstantColumnType)
    Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> codeBlock("%T()", Poet.kotlinOffsetDateTimeColumnType)
    Column.Type.Primitive.UUID -> codeBlock("%T()", Poet.uuidColumnType)
    Column.Type.Primitive.JSON -> codeBlock("%T()", c.poet.defaultJsonColumnType)
    Column.Type.Primitive.JSONB -> codeBlock("%T()", c.poet.defaultJsonColumnType)
    Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> codeBlock("%T()", c.poet.unconstrainedNumericColumnType)
}