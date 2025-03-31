package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.dsl.addInitializerBlock
import io.github.klahap.pgen.dsl.addProperty
import io.github.klahap.pgen.dsl.buildObject
import io.github.klahap.pgen.model.sql.Column
import io.github.klahap.pgen.model.sql.Table
import io.github.klahap.pgen.util.makeDifferent


context(CodeGenContext)
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
            type = Poet.column.parameterizedBy(
                when (column.type) {
                    is Column.Type.NonPrimitive.Array -> List::class.asTypeName()
                        .parameterizedBy(column.type.getTypeName())

                    else -> column.type.getTypeName()
                }.copy(nullable = column.isNullable),
            ),
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
}
