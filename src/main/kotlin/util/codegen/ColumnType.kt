package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.model.sql.Column
import java.math.BigDecimal
import java.util.UUID


context(CodeGenContext)
fun Column.Type.getTypeName(innerArrayType: Boolean = true): TypeName = when (this) {
    is Column.Type.NonPrimitive.Array -> if (innerArrayType)
        elementType.getTypeName()
    else
        List::class.asTypeName().parameterizedBy(elementType.getTypeName())

    is Column.Type.NonPrimitive.Domain -> getDomainTypename()
    is Column.Type.NonPrimitive.Reference -> getValueClass().name.poet
    is Column.Type.NonPrimitive.Enum -> name.typeName
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
    Column.Type.Primitive.INT4MULTIRANGE -> typeNameMultiRange.parameterizedBy(Int::class.asTypeName())
    Column.Type.Primitive.INT8MULTIRANGE -> typeNameMultiRange.parameterizedBy(Long::class.asTypeName())
    Column.Type.Primitive.INT4 -> Int::class.asTypeName()
    Column.Type.Primitive.FLOAT4 -> Float::class.asTypeName()
    Column.Type.Primitive.FLOAT8 -> Double::class.asTypeName()
    Column.Type.Primitive.JSON -> Poet.jsonElement
    Column.Type.Primitive.JSONB -> Poet.jsonElement
    Column.Type.Primitive.INT2 -> Short::class.asTypeName()
    Column.Type.Primitive.TEXT -> String::class.asTypeName()
    Column.Type.Primitive.TIME -> Poet.localTime
    Column.Type.Primitive.TIMESTAMP -> Poet.instant
    Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE -> Poet.offsetDateTime
    Column.Type.Primitive.UUID -> UUID::class.asTypeName()
    Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> BigDecimal::class.asTypeName()
}

private fun codeBlock(format: String, vararg args: Any) = CodeBlock.builder().add(format, *args).build()

context(CodeGenContext)
fun Column.Type.getExposedColumnType(): CodeBlock = when (this) {
    is Column.Type.NonPrimitive.Array ->
        codeBlock("%T(%L)", getArrayColumnType, elementType.getExposedColumnType())

    is Column.Type.NonPrimitive.Composite ->
        codeBlock("%T(sqlType=%S)", getColumnTypeTypeName(), sqlType)

    is Column.Type.NonPrimitive.Enum ->
        codeBlock("%T(%T::class)", ClassName("org.jetbrains.exposed.sql", "EnumerationColumnType"), name.typeName)

    is Column.Type.NonPrimitive.Numeric ->
        codeBlock(
            "%T(precision = $precision, scale = $scale)",
            ClassName("org.jetbrains.exposed.sql", "DecimalColumnType")
        )

    is Column.Type.NonPrimitive.Domain ->
        codeBlock("%T(kClass=%T::class, sqlType=%S)", domainTypeColumn, getDomainTypename(), sqlType)

    is Column.Type.NonPrimitive.Reference ->
        codeBlock("%T(kClass=%T::class, sqlType=%S)", domainTypeColumn, getValueClass().name.poet, originalType.sqlType)

    Column.Type.Primitive.INT8 -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "LongColumnType"))
    Column.Type.Primitive.BOOL -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "BooleanColumnType"))
    Column.Type.Primitive.BINARY -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "BinaryColumnType"))
    Column.Type.Primitive.VARCHAR -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "TextColumnType"))
    Column.Type.Primitive.DATE ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinLocalDateColumnType"))

    Column.Type.Primitive.INTERVAL ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinDurationColumnType"))

    Column.Type.Primitive.INT4RANGE -> codeBlock("%T()", typeNameInt4RangeColumnType)
    Column.Type.Primitive.INT8RANGE -> codeBlock("%T()", typeNameInt8RangeColumnType)
    Column.Type.Primitive.INT4MULTIRANGE -> codeBlock("%T()", typeNameInt4MultiRangeColumnType)
    Column.Type.Primitive.INT8MULTIRANGE -> codeBlock("%T()", typeNameInt8MultiRangeColumnType)
    Column.Type.Primitive.INT4 -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "IntegerColumnType"))
    Column.Type.Primitive.FLOAT4 -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "FloatColumnType"))
    Column.Type.Primitive.FLOAT8 -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "DoubleColumnType"))
    Column.Type.Primitive.JSON -> codeBlock("%T()", defaultJsonColumnType)
    Column.Type.Primitive.JSONB -> codeBlock("%T()", defaultJsonColumnType)
    Column.Type.Primitive.INT2 -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "ShortColumnType"))
    Column.Type.Primitive.TEXT -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "TextColumnType"))
    Column.Type.Primitive.TIME ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinLocalTimeColumnType"))

    Column.Type.Primitive.TIMESTAMP ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinInstantColumnType"))

    Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinOffsetDateTimeColumnType"))

    Column.Type.Primitive.UUID -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "UUIDColumnType"))
    Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> codeBlock("%T()", typeNameUnconstrainedNumericColumnType)
}