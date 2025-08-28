package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.klahap.pgen.dsl.addCode
import io.github.klahap.pgen.dsl.addFunction
import io.github.klahap.pgen.dsl.addProperty
import io.github.klahap.pgen.dsl.buildDataClass
import io.github.klahap.pgen.dsl.buildObject
import io.github.klahap.pgen.dsl.primaryConstructor
import io.github.klahap.pgen.model.sql.Column
import io.github.klahap.pgen.model.sql.CompositeType


context(c: CodeGenContext)
internal fun CompositeType.toTypeSpecInternal() = buildDataClass(this@toTypeSpecInternal.name.prettyName) {
    primaryConstructor {
        this@toTypeSpecInternal.columns.forEach { column ->
            val type = column.getColumnTypeName()
            addParameter(column.prettyName, type)
            addProperty(name = column.prettyName, type = type) {
                initializer(column.prettyName)
            }
        }
    }

    addType(
        buildObject("ColumnType") {
            superclass(
                Poet.columnType
                    .parameterizedBy(this@toTypeSpecInternal.name.typeName)
            )
            addFunction("sqlType") {
                addModifiers(KModifier.OVERRIDE)
                returns(String::class)
                addCode("return %S", this@toTypeSpecInternal.type.sqlType)
            }
            addFunction("valueFromDB") {
                addModifiers(KModifier.OVERRIDE)
                addParameter("value", Any::class)
                returns(this@toTypeSpecInternal.name.typeName)
                addCode {
                    beginControlFlow("val fields = when (value)")
                    add("is PGobject -> %T.parseFields(value.value ?: \"\")\n", c.poet.pgStructField)
                    add("is String -> %T.parseFields(value)\n", c.poet.pgStructField)
                    add(
                        "else -> error(\"Unexpected value for " +
                                "${this@toTypeSpecInternal.name.prettyName}: ${'$'}value\")\n"
                    )
                    endControlFlow()
                    add(
                        "if (fields.size != ${this@toTypeSpecInternal.columns.size}) error(%S)\n",
                        "unexpected number of fields"
                    )
                    add("return ${this@toTypeSpecInternal.name.prettyName} (\n")
                    this@toTypeSpecInternal.columns.sortedBy { it.pos }.forEachIndexed { idx, column ->
                        add("  %L = ", column.prettyName)
                        addPgFieldConverter(column.type)
                        add(".deserialize(fields[%L]),\n", idx)
                    }
                    add(")")
                }
            }
            addFunction("notNullValueToDB") {
                addModifiers(KModifier.OVERRIDE)
                addParameter("value", this@toTypeSpecInternal.name.typeName)
                returns(Any::class)
                addCode {
                    beginControlFlow("val dataStr = buildList")
                    this@toTypeSpecInternal.columns.sortedBy { it.pos }.forEach { column ->
                        add("add(")
                        addPgFieldConverter(column.type)
                        add(".serialize(value.%L))\n", column.prettyName)
                    }
                    endControlFlow()
                    beginControlFlow("return %T().apply", Poet.PGobject)
                    add("this.value = dataStr.%T()\n", c.poet.pgStructFieldJoin)
                    add("this.type = sqlType()\n")
                    endControlFlow()
                }
            }
        }
    )
}

context(c: CodeGenContext)
private fun CodeBlock.Builder.addPgFieldConverter(type: Column.Type) = when (type) {
    Column.Type.Primitive.BOOL,
    Column.Type.Primitive.DATE,
    Column.Type.Primitive.FLOAT4,
    Column.Type.Primitive.FLOAT8,
    Column.Type.Primitive.INT4RANGE,
    Column.Type.Primitive.INT8RANGE,
    Column.Type.Primitive.INT4MULTIRANGE,
    Column.Type.Primitive.INT8MULTIRANGE,
    Column.Type.Primitive.INTERVAL,
    Column.Type.Primitive.JSON,
    Column.Type.Primitive.JSONB,
    Column.Type.Primitive.TIME,
    Column.Type.Primitive.TIMESTAMP,
    Column.Type.Primitive.TIMESTAMP_WITH_TIMEZONE,
    is Column.Type.NonPrimitive.Array,
    is Column.Type.NonPrimitive.PgVector,
    is Column.Type.NonPrimitive.Composite,
    is Column.Type.NonPrimitive.Domain,
    is Column.Type.NonPrimitive.Reference ->
        throw NotImplementedError("Unsupported composite field type ${type.sqlType}")

    is Column.Type.NonPrimitive.Enum -> add("%T.Enum(%T::class)", c.poet.pgStructFieldConverter, type.getTypeName())
    is Column.Type.NonPrimitive.Numeric -> add("%T.BigDecimal", c.poet.pgStructFieldConverter)
    Column.Type.Primitive.INT2 -> add("%T.Small", c.poet.pgStructFieldConverter)
    Column.Type.Primitive.INT4 -> add("%T.Int", c.poet.pgStructFieldConverter)
    Column.Type.Primitive.INT8 -> add("%T.Long", c.poet.pgStructFieldConverter)
    Column.Type.Primitive.TEXT -> add("%T.String", c.poet.pgStructFieldConverter)
    Column.Type.Primitive.UUID -> add("%T.Uuid", c.poet.pgStructFieldConverter)
    Column.Type.Primitive.VARCHAR -> add("%T.String", c.poet.pgStructFieldConverter)
    Column.Type.Primitive.UNCONSTRAINED_NUMERIC -> add("%T.BigDecimal", c.poet.pgStructFieldConverter)
    Column.Type.Primitive.BINARY -> add("%T.ByteArray", c.poet.pgStructFieldConverter)
}
