package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.dsl.addCompanionObject
import io.github.klahap.pgen.dsl.addEnumConstant
import io.github.klahap.pgen.dsl.addProperty
import io.github.klahap.pgen.dsl.buildEnum
import io.github.klahap.pgen.dsl.primaryConstructor
import io.github.klahap.pgen.model.config.Config
import io.github.klahap.pgen.model.sql.Enum
import io.github.klahap.pgen.util.toSnakeCase

context(c: CodeGenContext)
internal fun Enum.toTypeSpecInternal() = buildEnum(this@toTypeSpecInternal.name.prettyName) {
    addSuperinterface(c.poet.pgEnum)
    primaryConstructor {
        addParameter("pgEnumLabel", String::class)
        addProperty(name = "pgEnumLabel", type = String::class.asTypeName()) {
            addModifiers(KModifier.OVERRIDE)
            initializer("pgEnumLabel")
        }
    }
    this@toTypeSpecInternal.fields.forEach { field ->
        val enumName = when (c.connectionType) {
            Config.ConnectionType.JDBC -> field.toSnakeCase(uppercase = true)
            Config.ConnectionType.R2DBC -> field
        }
        addEnumConstant(enumName) {
            addSuperclassConstructorParameter("pgEnumLabel = %S", field)
        }
    }
    val pgEnumTypeNameValue = "${this@toTypeSpecInternal.name.schema.schemaName}.${this@toTypeSpecInternal.name.name}"
    addProperty(name = "pgEnumTypeName", type = String::class.asTypeName()) {
        initializer("%S", pgEnumTypeNameValue)
        addModifiers(KModifier.OVERRIDE)
    }

    if (c.connectionType == Config.ConnectionType.R2DBC)
        addCompanionObject {
            addProperty(name = "codec", type = Poet.codecRegistrar) {
                initializer(
                    "%T.builder().withEnum(%S.lowercase(), " +
                            "${this@toTypeSpecInternal.name.prettyName}::class.java).build()",
                    ClassName("io.r2dbc.postgresql.codec", "EnumCodec"),
                    this@toTypeSpecInternal.name.name.lowercase(),
                )
            }
        }
}
