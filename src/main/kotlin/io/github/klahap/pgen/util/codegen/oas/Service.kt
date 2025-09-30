package io.github.klahap.pgen.util.codegen.oas

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.UNIT
import io.github.klahap.pgen.dsl.addCode
import io.github.klahap.pgen.dsl.addFunction
import io.github.klahap.pgen.dsl.addInterface
import io.github.klahap.pgen.dsl.addParameter
import io.github.klahap.pgen.model.config.Config
import io.github.klahap.pgen.model.oas.TableOasData
import io.github.klahap.pgen.util.codegen.CodeGenContext
import io.github.klahap.pgen.util.codegen.Poet
import io.github.klahap.pgen.util.codegen.getColumnTypeName

context(c: CodeGenContext, mapperConfig: Config.OasConfig.Mapper)
fun FileSpec.Builder.addTableService(data: TableOasData) = addInterface(data.getOasServiceName()) {

    val idType = data.sqlData.columns.singleOrNull { it.prettyName == "id" }
        // TODO check if id is primary key
        ?.getColumnTypeName()
        ?: error("no id column found")

    addProperty("db", Poet.dbR2dbc)

    addFunction("getById") {
        addModifiers(KModifier.SUSPEND)
        addParameter("id", idType)
        returns(data.sqlData.entityTypeName)
        addCode(
            """
            return getAll { %T.id eq id }
                .%T()
                ?: throw %T.NotFound("${data.namePretty} not found with id: ${'$'}id")
            """.trimIndent(),
            data.sqlData.name.typeName,
            Poet.flowSingleOrNull,
            Poet.quatiException,
        )
    }

    addFunction("getAll") {
        addModifiers(KModifier.SUSPEND)
        addParameter(
            "filter",
            LambdaTypeName.get(Poet.sqlExpressionBuilder, emptyList(), Poet.opBoolean).copy(nullable = true),
        ) {
            defaultValue("null")
        }
        returns(Poet.flow.parameterizedBy(data.sqlData.entityTypeName))
        addCode {
            beginControlFlow("return %T", Poet.channelFlow)
            beginControlFlow("%T(db = db, readOnly = true)", Poet.r2dbcSuspendTransaction)
            add(
                """
                %T.%T()
                .let {
                    when (filter) {
                        null -> it
                        else -> it.where(filter)
                    }
                }
                .%T(%T.Entity::create)
                .collect { send(it) }
            """.trimIndent(),
                data.sqlData.name.typeName,
                Poet.r2dbcSelectAll,
                Poet.flowMap,
                data.sqlData.name.typeName,
            )
            endControlFlow()
            endControlFlow()
        }
    }

    addFunction("create") {
        addModifiers(KModifier.SUSPEND)
        addParameter("data", data.sqlData.updateEntityTypeName)
        returns(data.sqlData.entityTypeName)
        addCode {
            beginControlFlow("return %T(db = db)", Poet.r2dbcSuspendTransaction)
            add(
                """
                %T.%T(ignoreErrors = true) {
                    it.%T(data)
                }.%T()
                    .let { it ?: throw %T.BadRequest("Cannot create ${data.namePretty}") }
                    .let(%T.Entity::create)
            """.trimIndent(),
                data.sqlData.name.typeName,
                Poet.r2dbcInsertReturning,
                data.sqlData.updateEntitySetFunctionTypeName,
                Poet.flowSingleOrNull,
                Poet.quatiException,
                data.sqlData.name.typeName,
            )
            endControlFlow()
        }
    }

    addFunction("delete") {
        addModifiers(KModifier.SUSPEND)
        addParameter("id", idType)
        returns(UNIT)
        addCode {
            beginControlFlow("return %T(db = db)", Poet.r2dbcSuspendTransaction)
            add(
                "%T.%T { %T.id %T id }\n",
                data.sqlData.name.typeName,
                Poet.r2dbcDeleteWhere,
                data.sqlData.name.typeName,
                Poet.eq
            )
            endControlFlow()
            beginControlFlow(".let")
            add(
                """if (it == 0) throw %T.NotFound("Cannot delete ${data.namePretty} with id: ${'$'}id")${'\n'}""",
                Poet.quatiException,
            )
            add("Unit\n")
            endControlFlow()
        }
    }

    addFunction("update") {
        addModifiers(KModifier.SUSPEND)
        addParameter("id", idType)
        addParameter("data", data.sqlData.updateEntityTypeName)
        returns(data.sqlData.entityTypeName)
        addCode {
            beginControlFlow("return %T(db = db)", Poet.r2dbcSuspendTransaction)
            add(
                """
                %T.%T(where = { %T.id eq id }) {
                    it.%T(data)
                }.%T()
                    .let { it ?: throw %T.BadRequest("Cannot update ${data.namePretty} with id: ${'$'}id") }
                    .let(%T.Entity::create)
                """.trimIndent(),
                data.sqlData.name.typeName,
                Poet.r2dbcUpdateReturning,
                data.sqlData.name.typeName,
                data.sqlData.updateEntitySetFunctionTypeName,
                Poet.flowSingleOrNull,
                Poet.quatiException,
                data.sqlData.name.typeName,
            )
            endControlFlow()
        }
    }
}