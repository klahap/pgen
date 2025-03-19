package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.KModifier
import io.github.klahap.pgen.dsl.addFunction
import io.github.klahap.pgen.dsl.addProperty
import io.github.klahap.pgen.dsl.buildValueClass
import io.github.klahap.pgen.dsl.primaryConstructor
import io.github.klahap.pgen.model.sql.Table.Column.Type.NonPrimitive.Domain

context(CodeGenContext)
internal fun Domain.toTypeSpecInternal() = buildValueClass(this@toTypeSpecInternal.name.prettyName) {
    val dataFieldName = "value"

    val typename = originalType.getTypeName()
    primaryConstructor {
        addParameter(dataFieldName, typename)
        addProperty(name = dataFieldName, type = typename) {
            initializer(dataFieldName)
        }
    }
    addFunction(name = "toString") {
        addModifiers(KModifier.OVERRIDE)
        returns(String::class)
        addCode("return $dataFieldName.toString()")
    }
}
