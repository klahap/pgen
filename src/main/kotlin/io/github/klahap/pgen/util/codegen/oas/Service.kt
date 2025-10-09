package io.github.klahap.pgen.util.codegen.oas

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
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

@OptIn(ExperimentalKotlinPoetApi::class)
context(c: CodeGenContext, mapperConfig: Config.OasConfig.Mapper)
fun FileSpec.Builder.addTableService(data: TableOasData) = addInterface(data.getOasServiceName()) {

    val idType = data.sqlData.columns.singleOrNull { it.prettyName == "id" }
        // TODO check if id is primary key
        ?.getColumnTypeName()
        ?: error("no id column found")

    addProperty("db", Poet.dbR2dbc)

    addFunction("getById") {
        val localConfigContext = c.localConfigContext?.takeIf { Config.OasConfig.CRUD.READ in it.atMethods }
        localConfigContext?.also { contextParameter("c", it.type) }
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
        val localConfigContext = c.localConfigContext?.takeIf { Config.OasConfig.CRUD.READ in it.atMethods }
        localConfigContext?.also { contextParameter("c", it.type) }
        addModifiers(KModifier.SUSPEND)
        addParameter(
            "fieldSetMapper",
            LambdaTypeName.get(null, listOf(ParameterSpec("fieldSet", Poet.fieldSet)), Poet.fieldSet)
                .copy(nullable = true),
        ) {
            defaultValue("null")
        }
        addParameter(
            "queryMapper",
            LambdaTypeName.get(null, listOf(ParameterSpec("query", Poet.query)), Poet.query).copy(nullable = true),
        ) {
            defaultValue("null")
        }
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
                ${"// ".takeIf { localConfigContext == null } ?: ""}%T(c)
                %T
                    .let { if (fieldSetMapper == null) it else fieldSetMapper(it) }
                    .%T()
                    .let { if (filter == null) it else it.where(filter) }
                    .let { if (queryMapper == null) it else queryMapper(it) }
                    .%T(%T.Entity::create)
                    .collect { send(it) }
            """.trimIndent(),
                c.poet.setLocalConfig,
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
        val localConfigContext = c.localConfigContext?.takeIf { Config.OasConfig.CRUD.CREATE in it.atMethods }
        localConfigContext?.also { contextParameter("c", it.type) }
        addModifiers(KModifier.SUSPEND)
        addParameter("data", data.sqlData.updateEntityTypeName)
        returns(data.sqlData.entityTypeName)
        addCode {
            beginControlFlow("return %T(db = db)", Poet.r2dbcSuspendTransaction)
            add(
                """
                ${"// ".takeIf { localConfigContext == null } ?: ""}%T(c)
                %T.%T(ignoreErrors = true) {
                    it.%T(data)
                }.%T()
                    .let { it ?: throw %T.BadRequest("Cannot create ${data.namePretty}") }
                    .let(%T.Entity::create)
            """.trimIndent(),
                c.poet.setLocalConfig,
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
        val localConfigContext = c.localConfigContext?.takeIf { Config.OasConfig.CRUD.DELETE in it.atMethods }
        localConfigContext?.also { contextParameter("c", it.type) }
        addModifiers(KModifier.SUSPEND)
        addParameter("id", idType)
        returns(UNIT)
        addCode {
            beginControlFlow("return %T(db = db)", Poet.r2dbcSuspendTransaction)
            add(
                """
                ${"// ".takeIf { localConfigContext == null } ?: ""}%T(c)
                %T.%T { %T.id %T id }
                """.trimIndent(),
                c.poet.setLocalConfig,
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
        val localConfigContext = c.localConfigContext?.takeIf { Config.OasConfig.CRUD.UPDATE in it.atMethods }
        localConfigContext?.also { contextParameter("c", it.type) }
        addModifiers(KModifier.SUSPEND)
        addParameter("id", idType)
        addParameter("data", data.sqlData.updateEntityTypeName)
        returns(data.sqlData.entityTypeName)
        addCode {
            beginControlFlow("return %T(db = db)", Poet.r2dbcSuspendTransaction)
            add(
                """
                ${"// ".takeIf { localConfigContext == null } ?: ""}%T(c)
                %T.%T(where = { %T.id eq id }) {
                    it.%T(data)
                }.%T()
                    .let { it ?: throw %T.BadRequest("Cannot update ${data.namePretty} with id: ${'$'}id") }
                    .let(%T.Entity::create)
                """.trimIndent(),
                c.poet.setLocalConfig,
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