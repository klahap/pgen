package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
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
import io.github.klahap.pgen.util.toCamelCase

context(c: CodeGenContext)
internal fun Table.toTypeSpecInternal() = buildObject(this@toTypeSpecInternal.name.prettyName) {
    val foreignKeysSingle = this@toTypeSpecInternal.foreignKeys.map { it.toTyped() }
        .filterIsInstance<Table.ForeignKeyTyped.SingleKey>()
        .associateBy { it.reference.sourceColumn }
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
                foreignKeysSingle[column.name]?.let { fKey ->
                    append(".references(ref = %T.${fKey.reference.targetColumn.pretty}, fkName = %S)")
                    postArgs.add(fKey.targetTable.typeName)
                    postArgs.add(fKey.name)
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
                @Suppress("SpreadOperator") addStatement(
                    "foreignKey($foreignKeyStrFormat)",
                    *foreignKeyStrValues,
                )
            }
        }

    addType(toConstraintsObject())
    addType(toTypeSpecEntity())
    addType(toTypeSpecUpdateEntity())
}

context(c: CodeGenContext)
private fun Table.toConstraintsObject() = buildObject(this@toConstraintsObject.constraintsTypeName.simpleName) {
    fun addConstraint(
        name: String,
        clazz: ClassName,
        additionalFormat: String = "",
        additionalArgs: Array<Any> = arrayOf(),
    ) = addProperty(
        name = name.toCamelCase(capitalized = false),
        type = clazz,
    ) {
        @Suppress("SpreadOperator")
        initializer(
            "%T(table = %L, name = %S$additionalFormat)",
            clazz,
            this@toConstraintsObject.name.prettyName,
            name,
            *additionalArgs,
        )
    }

    this@toConstraintsObject.primaryKey?.also { pkey ->
        addConstraint(name = pkey.keyName, clazz = c.poet.pKeyConstraint)
    }
    this@toConstraintsObject.foreignKeys.forEach { fkey ->
        addConstraint(name = fkey.name, clazz = c.poet.fKeyConstraint)
    }
    this@toConstraintsObject.uniqueConstraints.forEach { name ->
        addConstraint(name = name, clazz = c.poet.uniqueConstraint)
    }
    this@toConstraintsObject.checkConstraints.forEach { name ->
        addConstraint(name = name, clazz = c.poet.checkConstraint)
    }
    this@toConstraintsObject.columns.filter { !it.isNullable }.forEach { column ->
        val name = column.name.value + "_not_null"
        val clazz = c.poet.notNullConstraint
        addProperty(
            name = name.toCamelCase(capitalized = false),
            type = clazz,
        ) {
            @Suppress("SpreadOperator")
            initializer(
                "%T(column = %L.%L, name = %S)",
                clazz,
                this@toConstraintsObject.name.prettyName,
                column.prettyName,
                name,
            )
        }
    }
}

context(c: CodeGenContext)
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
                        c.poet.getColumnWithAlias,
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

context(c: CodeGenContext)
private fun Table.toTypeSpecUpdateEntity(
) = buildDataClass(this@toTypeSpecUpdateEntity.updateEntityTypeName.simpleName) {
    primaryConstructor {
        this@toTypeSpecUpdateEntity.columns.forEach { column ->
            val innerType = column.getColumnTypeName()
            val type = Poet.option.parameterizedBy(innerType)
            addParameter(column.prettyName, type)
            addProperty(name = column.prettyName, type = type) {
                initializer(column.prettyName)
            }
        }
    }
    addCompanionObject {
        addFunction("set") {
            receiver(Poet.updateBuilder.parameterizedBy(STAR))
            addParameter(name = "entity", type = this@toTypeSpecUpdateEntity.updateEntityTypeName)
            addCode {
                this@toTypeSpecUpdateEntity.columns.forEach { column ->
                    add(
                        "entity.%L.%T()?.let { set(%T.%L, it.value) }\n",
                        column.prettyName,
                        Poet.optionTakeSome,
                        this@toTypeSpecUpdateEntity.name.typeName,
                        column.prettyName,
                    )
                }
            }
        }
    }
}
