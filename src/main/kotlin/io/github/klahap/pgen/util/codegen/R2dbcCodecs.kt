package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.dsl.addProperty
import io.github.klahap.pgen.model.sql.Enum


context(c: CodeGenContext) fun FileSpec.Builder.createCodecCollection(
    objs: Collection<Enum>,
) {
    this.addProperty(
        name = "allR2dbcCodecs",
        type = List::class.asTypeName().parameterizedBy(Poet.codecRegistrar)
    ) {
        @Suppress("SpreadOperator") initializer(
            objs.joinToString(prefix = "listOf(", postfix = ")", separator = ", ") { "%T.codec" },
            *(objs.map { it.name.typeName }.toTypedArray()),
        )
    }
}