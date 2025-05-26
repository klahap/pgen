package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import io.github.klahap.pgen.dsl.addCode
import io.github.klahap.pgen.dsl.addCompanionObject
import io.github.klahap.pgen.dsl.addFunction
import io.github.klahap.pgen.dsl.addInitializerBlock
import io.github.klahap.pgen.dsl.addParameter
import io.github.klahap.pgen.dsl.addProperty
import io.github.klahap.pgen.dsl.buildDataClass
import io.github.klahap.pgen.dsl.buildObject
import io.github.klahap.pgen.dsl.primaryConstructor
import io.github.klahap.pgen.model.sql.Table
import io.github.klahap.pgen.util.makeDifferent

context(_: CodeGenContext)
internal fun Table.toTypeSpecInternal() = buildObject(this@toTypeSpecInternal.name.prettyName) {
    val foreignKeysSingle = this@toTypeSpecInternal.foreignKeys.map { it.toTyped() }
        .filterIsInstance<Table.ForeignKeyTyped.SingleKey>()
        .associate { it.reference.sourceColumn to (it.targetTable to it.reference.targetColumn) }
    superclass(Poet.table)
    addSuperclassConstructorParameter(
        "%S",
        "${this@toTypeSpecInternal.name.schema.schemaName}.${this@toTypeSpecInternal.name.name}"
    )
    this@toTypeSpecInternal.columns.forEach { column ->
        addProperty(
            name = column.prettyName,
            type = Poet.column.parameterizedBy(column.getColumnTypeName()),
        ) {
            val postArgs = mutableListOf<Any>()
            val postfix = buildString {
                foreignKeysSingle[column.name]?.let { foreignKey ->
                    append(".references(%T.${foreignKey.second.pretty})")
                    postArgs.add(foreignKey.first.typeName)
                }
                if (column.isNullable)
                    append(".nullable()")
            }
            initializer(column, postfix = postfix, postArgs = postArgs)
        }
    }
    if (this@toTypeSpecInternal.primaryKey != null) {
        val columnNames = this@toTypeSpecInternal.columns.map { it.prettyName }
        addProperty(name = "primaryKey".makeDifferent(columnNames), type = Poet.primaryKey) {
            addModifiers(KModifier.OVERRIDE)
            initializer(
                "PrimaryKey(%L, name = %S)",
                this@toTypeSpecInternal.primaryKey.columnNames.joinToString(", ") { it.pretty },
                this@toTypeSpecInternal.primaryKey.keyName,
            )
        }
    }

    val foreignKeysMulti = this@toTypeSpecInternal.foreignKeys.map { it.toTyped() }
        .filterIsInstance<Table.ForeignKeyTyped.MultiKey>()
    if (foreignKeysMulti.isNotEmpty())
        addInitializerBlock {
            foreignKeysMulti.forEach { foreignKey ->
                val foreignKeyStrFormat = foreignKey.references.joinToString(", ") { ref ->
                    "${ref.sourceColumn.pretty} to %T.${ref.targetColumn.pretty}"
                }
                val foreignKeyStrValues = foreignKey.references.map {
                    foreignKey.targetTable.typeName
                }.toTypedArray()
                addStatement("foreignKey($foreignKeyStrFormat)", *foreignKeyStrValues)
            }
        }

    addType(toTypeSpecEntity())
}


context(codeGenContext: CodeGenContext)
private fun Table.toTypeSpecEntity() = buildDataClass(this@toTypeSpecEntity.entityTypeName.simpleName) {
    primaryConstructor {
        this@toTypeSpecEntity.columns.forEach { column ->
            val type = column.getColumnTypeName()
            addParameter(column.prettyName, type)
            addProperty(name = column.prettyName, type = type) {
                initializer(column.prettyName)
            }
        }
    }
    addCompanionObject {
        addFunction("create") {
            addParameter(name = "row", type = Poet.resultRow)
            addParameter(name = "alias", type = Poet.alias.parameterizedBy(STAR).copy(nullable = true)) {
                this.defaultValue("null")
            }
            returns(this@toTypeSpecEntity.entityTypeName)
            addCode {
                add("return %T(\n", this@toTypeSpecEntity.entityTypeName)
                this@toTypeSpecEntity.columns.forEach { column ->
                    add(
                        "  %L = row.%T(%T.%L, alias),\n",
                        column.prettyName,
                        codeGenContext.poet.getColumnWithAlias,
                        this@toTypeSpecEntity.name.typeName,
                        column.prettyName,
                    )
                }
                add(")")
            }
        }

        addFunction("set") {
            receiver(Poet.updateBuilder.parameterizedBy(STAR))
            addParameter(name = "entity", type = this@toTypeSpecEntity.entityTypeName)
            addCode {
                this@toTypeSpecEntity.columns.forEach { column ->
                    add(
                        "set(%T.%L, entity.%L)\n",
                        this@toTypeSpecEntity.name.typeName,
                        column.prettyName,
                        column.prettyName,
                    )
                }
            }
        }
    }
}
