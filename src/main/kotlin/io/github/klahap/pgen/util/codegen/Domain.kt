package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.dsl.addFunction
import io.github.klahap.pgen.dsl.addProperty
import io.github.klahap.pgen.dsl.buildValueClass
import io.github.klahap.pgen.dsl.primaryConstructor
import io.github.klahap.pgen.model.sql.Column.Type.NonPrimitive.Domain

context(c: CodeGenContext)
internal fun Domain.toTypeSpecInternal() = buildValueClass(this@toTypeSpecInternal.name.prettyName) {
    val dataFieldName = "value"

    val typename = originalType.getTypeName()
    val isStringLike = typename == String::class.asTypeName()
    if (isStringLike)
        addSuperinterface(c.poet.stringLike)
    primaryConstructor {
        addParameter(dataFieldName, typename)
        addProperty(name = dataFieldName, type = typename) {
            initializer(dataFieldName)
            if (isStringLike)
                addModifiers(KModifier.OVERRIDE)
        }
    }
    addFunction(name = "toString") {
        addModifiers(KModifier.OVERRIDE)
        returns(String::class)
        addCode("return $dataFieldName.toString()")
    }
}
