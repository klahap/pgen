package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.model.sql.Table
import java.math.BigDecimal
import java.util.UUID


context(CodeGenContext)
fun Table.Column.Type.getTypeName(innerArrayType: Boolean = true): TypeName = when (this) {
    is Table.Column.Type.NonPrimitive.Array -> if (innerArrayType)
        elementType.getTypeName()
    else
        List::class.asTypeName().parameterizedBy(elementType.getTypeName())

    is Table.Column.Type.NonPrimitive.Domain -> getDomainTypename()
    is Table.Column.Type.NonPrimitive.Reference -> clazz.poet
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

private fun codeBlock(format: String, vararg args: Any) = CodeBlock.builder().add(format, *args).build()

context(CodeGenContext)
fun Table.Column.Type.getExposedColumnType(): CodeBlock = when (this) {
    is Table.Column.Type.NonPrimitive.Array ->
        codeBlock("%T(%L)", getArrayColumnType, elementType.getExposedColumnType())

    is Table.Column.Type.NonPrimitive.Enum ->
        codeBlock("%T(%T::class)", ClassName("org.jetbrains.exposed.sql", "EnumerationColumnType"), name.typeName)

    is Table.Column.Type.NonPrimitive.Numeric ->
        codeBlock(
            "%T(precision = $precision, scale = $scale)",
            ClassName("org.jetbrains.exposed.sql", "DecimalColumnType")
        )

    is Table.Column.Type.NonPrimitive.Domain ->
        codeBlock("%T(kClass=%T::class, sqlType=%S)", domainTypeColumn, getDomainTypename(), sqlType)

    is Table.Column.Type.NonPrimitive.Reference ->
        codeBlock("%T(kClass=%T::class, sqlType=%S)", domainTypeColumn, clazz.poet, originalType.sqlType)

    Table.Column.Type.Primitive.INT8 -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "LongColumnType"))
    Table.Column.Type.Primitive.BOOL -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "BooleanColumnType"))
    Table.Column.Type.Primitive.BINARY -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "BinaryColumnType"))
    Table.Column.Type.Primitive.VARCHAR -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "TextColumnType"))
    Table.Column.Type.Primitive.DATE ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinLocalDateColumnType"))

    Table.Column.Type.Primitive.INTERVAL ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinDurationColumnType"))

    Table.Column.Type.Primitive.INT4RANGE -> codeBlock("%T()", typeNameInt4RangeColumnType)
    Table.Column.Type.Primitive.INT8RANGE -> codeBlock("%T()", typeNameInt8RangeColumnType)
    Table.Column.Type.Primitive.INT4MULTIRANGE -> codeBlock("%T()", typeNameInt4MultiRangeColumnType)
    Table.Column.Type.Primitive.INT8MULTIRANGE -> codeBlock("%T()", typeNameInt8MultiRangeColumnType)
    Table.Column.Type.Primitive.INT4 -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "IntegerColumnType"))
    Table.Column.Type.Primitive.JSON -> codeBlock("%T()", defaultJsonColumnType)
    Table.Column.Type.Primitive.JSONB -> codeBlock("%T()", defaultJsonColumnType)
    Table.Column.Type.Primitive.INT2 -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "ShortColumnType"))
    Table.Column.Type.Primitive.TEXT -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "TextColumnType"))
    Table.Column.Type.Primitive.TIME ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinLocalTimeColumnType"))

    Table.Column.Type.Primitive.TIMESTAMP ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinInstantColumnType"))

    Table.Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE ->
        codeBlock("%T()", ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "KotlinOffsetDateTimeColumnType"))

    Table.Column.Type.Primitive.UUID -> codeBlock("%T()", ClassName("org.jetbrains.exposed.sql", "UUIDColumnType"))
    Table.Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> codeBlock("%T()", typeNameUnconstrainedNumericColumnType)
}