package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.buildCodeBlock
import io.github.klahap.pgen.dsl.addClass
import io.github.klahap.pgen.dsl.addCode
import io.github.klahap.pgen.dsl.addControlFlow
import io.github.klahap.pgen.dsl.addFunction
import io.github.klahap.pgen.dsl.addProperty
import io.github.klahap.pgen.dsl.indent
import io.github.klahap.pgen.model.sql.Statement
import io.github.klahap.pgen.dsl.primaryConstructor
import io.github.klahap.pgen.util.makeDifferent

context(CodeGenContext)
internal val Collection<Statement>.packageName
    get() = map { it.name.packageName }.distinct().singleOrNull()
        ?: error("statements from different DB's cannot wirte in the same file")

context(CodeGenContext)
internal fun FileSpec.Builder.addStatements(statements: Collection<Statement>) {
    val packageName = statements.packageName
    statements.map { statement ->
        addClass(statement.name.prettyResultClassName) {
            addModifiers(KModifier.DATA)
            primaryConstructor {
                statement.columns.forEachIndexed { idx, column ->
                    val name = column.name.pretty
                    val type = column.type.getTypeName(innerArrayType = false).copy(nullable = column.isNullable)
                    addParameter(name, type)
                    addProperty(name = name, type = type) { initializer(name) }
                }
            }
        }
    }
    statements.map { statement ->
        val resultTypeName = ClassName(packageName.name, statement.name.prettyResultClassName)
        val statementNames = statement.columns.map { it.name.pretty }.toSet() +
                statement.variables.map { it.pretty }.toSet()
        addFunction(statement.name.prettyName) {
            when (statement.cardinality) {
                Statement.Cardinality.ONE -> addModifiers(KModifier.SUSPEND)
                Statement.Cardinality.MANY -> Unit
            }
            receiver(Poet.transaction)
            statement.variableTypes.entries.sortedBy { it.key }.forEach { (name, type) ->
                addParameter(name.pretty, type.getTypeName())
            }
            when (statement.cardinality) {
                Statement.Cardinality.ONE -> returns(resultTypeName)
                Statement.Cardinality.MANY -> returns(Poet.flow.parameterizedBy(resultTypeName))
            }

            addCode {
                val rowSetName = "rowSet".makeDifferent(statementNames)

                val inputsPairs = buildCodeBlock {
                    val inputs = statement.variables.map { name ->
                        val type = statement.variableTypes[name]!!
                        add("%L to %L,", type.getExposedColumnType(), name.pretty)
                    }
                }
                addControlFlow("return %T", Poet.generateChannelFlow) {
                    add("exec(stmt = %S, args = listOf(%L)) { %L ->\n", statement.sql, inputsPairs, rowSetName)
                    indent {
                        addControlFlow("while(%L.next())", rowSetName) {
                            add("%T(\n", Poet.trySendBlocking)
                            indent {
                                add("%T(\n", resultTypeName)
                                indent {
                                    statement.columns.forEachIndexed { idx, column ->
                                        add(
                                            "%L = %L.getObject(%L)%L.let { %L.valueFromDB(it) }%L,\n",
                                            column.name.pretty,
                                            rowSetName, idx + 1,
                                            if (column.isNullable) "?" else "!!",
                                            column.type.getExposedColumnType(),
                                            if (column.isNullable) "" else "!!",
                                        )
                                    }
                                }
                                add(")\n")
                            }
                            add(")\n")
                        }
                    }
                }
                when (statement.cardinality) {
                    Statement.Cardinality.ONE -> add("}.%T()\n", Poet.flowSingle)
                    Statement.Cardinality.MANY -> add("}\n")
                }
            }
        }
    }
}
