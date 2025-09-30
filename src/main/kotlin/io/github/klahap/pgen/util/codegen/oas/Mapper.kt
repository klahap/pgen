package io.github.klahap.pgen.util.codegen.oas

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.dsl.addCode
import io.github.klahap.pgen.dsl.addFunction
import io.github.klahap.pgen.dsl.addParameter
import io.github.klahap.pgen.model.config.Config
import io.github.klahap.pgen.model.oas.EnumOasData
import io.github.klahap.pgen.model.oas.TableFieldTypeOasData
import io.github.klahap.pgen.model.oas.TableOasData
import io.github.klahap.pgen.util.codegen.CodeGenContext
import io.github.klahap.pgen.util.codegen.Poet

context(c: CodeGenContext, mapperConfig: Config.OasConfig.Mapper)
fun FileSpec.Builder.addEnumMapper(data: EnumOasData) {
    addFunction("toDto") {
        val dstType = data.getOasType()
        val srcType = data.sqlData.name.typeName
        returns(dstType)
        receiver(srcType)
        addCode {
            beginControlFlow("return when (this)")
            data.items.forEach { item ->
                add("%T.%L -> %T.%L\n", srcType, item, dstType, item)
            }
            endControlFlow()
        }
    }
    addFunction("toEntity") {
        val srcType = data.getOasType()
        val dstType = data.sqlData.name.typeName
        returns(dstType)
        receiver(srcType)
        addCode {
            beginControlFlow("return when (this)")
            data.items.forEach { item ->
                add("%T.%L -> %T.%L\n", srcType, item, dstType, item)
            }
            endControlFlow()
        }
    }
}


context(c: CodeGenContext, mapperConfig: Config.OasConfig.Mapper)
fun FileSpec.Builder.addTableMapper(data: TableOasData) {
    addFunction("toDto") {
        val srcType = data.sqlData.entityTypeName
        val dstType = data.getOasReadType()
        receiver(srcType)
        returns(dstType)
        addCode {
            add("return %T(\n", dstType)
            data.fields.forEach { field ->
                add(
                    "    %L = %L%L,\n",
                    field.name,
                    field.sqlData.prettyName,
                    when (field.type) {
                        is TableFieldTypeOasData.Enum -> ".toDto()"
                        else -> ""
                    },
                )
            }
            add(")")
        }
    }

    addFunction("toUpdateEntity") {
        val srcType = data.getOasCreateType()
        val dstType = data.sqlData.updateEntityTypeName
        receiver(srcType)
        returns(dstType)
        addParameter(
            "block",
            LambdaTypeName.get(dstType, emptyList(), Unit::class.asTypeName()),
        ) {
            defaultValue("{}")
        }
        addCode {
            add("return %T(\n", dstType)
            val unsetFieldNames = data.sqlData.columns.map { it.prettyName }.toSet() -
                    data.fieldsAtCreate.map { it.name }.toSet()
            unsetFieldNames.forEach { field ->
                add(
                    "    %L = %T.Undefined,\n",
                    field,
                    Poet.option,
                )
            }
            data.fieldsAtCreate.forEach { field ->
                add(
                    "    %L = %T.Some(%L%L),\n",
                    field.name,
                    Poet.option,
                    field.sqlData.prettyName,
                    when (field.type) {
                        is TableFieldTypeOasData.Enum -> ".toEntity()"
                        else -> ""
                    },
                )
            }
            add(").apply(block)")
        }
    }

    addFunction("toUpdateEntity") {
        val srcType = data.getOasUpdateType()
        val dstType = data.sqlData.updateEntityTypeName
        receiver(srcType)
        returns(dstType)
        addParameter(
            "block",
            LambdaTypeName.get(dstType, emptyList(), Unit::class.asTypeName()),
        ) {
            defaultValue("{}")
        }
        addCode {
            add("return %T(\n", dstType)
            val unsetFieldNames = data.sqlData.columns.map { it.prettyName }.toSet() -
                    data.fieldsAtUpdate.map { it.name }.toSet()
            unsetFieldNames.forEach { field ->
                add(
                    "    %L = %T.Undefined,\n",
                    field,
                    Poet.option,
                )
            }
            data.fieldsAtUpdate.forEach { field ->
                add(
                    "    %L = %L",
                    field.name,
                    field.sqlData.prettyName,
                )
                when (field.type) {
                    is TableFieldTypeOasData.Enum -> add(".%T { it.toEntity() }", Poet.optionMap)
                    else -> Unit
                }
                add(",\n")
            }
            add(").apply(block)")
        }
    }
}
