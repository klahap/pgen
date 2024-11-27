package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.*
import io.github.klahap.pgen.dsl.*
import io.github.klahap.pgen.model.sql.Enum
import io.github.klahap.pgen.util.toSnakeCase

context(CodeGenContext)
internal fun Enum.toTypeSpecInternal() = buildEnum(this@toTypeSpecInternal.name.prettyName) {
    addSuperinterface(typeNamePgEnum)
    primaryConstructor {
        addParameter("pgEnumLabel", String::class)
        addProperty(name = "pgEnumLabel", type = String::class.asTypeName()) {
            addModifiers(KModifier.OVERRIDE)
            initializer("pgEnumLabel")
        }
    }
    this@toTypeSpecInternal.fields.forEach { field ->
        addEnumConstant(field.toSnakeCase(uppercase = true)) {
            addSuperclassConstructorParameter("pgEnumLabel = %S", field)
        }
    }
    val pgEnumTypeNameValue = "${this@toTypeSpecInternal.name.schema}.${this@toTypeSpecInternal.name.name}"
    addProperty(name = "pgEnumTypeName", type = String::class.asTypeName()) {
        initializer("%S", pgEnumTypeNameValue)
        addModifiers(KModifier.OVERRIDE)
    }
}