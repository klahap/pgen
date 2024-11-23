package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.dsl.addInitializerBlock
import io.github.klahap.pgen.dsl.addProperty
import io.github.klahap.pgen.dsl.buildObject
import io.github.klahap.pgen.model.sql.Table
import io.github.klahap.pgen.util.makeDifferent


context(CodeGenContext)
internal fun Table.toTypeSpecInternal() = buildObject(this@toTypeSpecInternal.name.prettyName) {
    superclass(Poet.table)
    addSuperclassConstructorParameter("%S", this@toTypeSpecInternal.name.name)
    this@toTypeSpecInternal.columns.forEach { column ->
        addProperty(
            name = column.prettyName,
            type = Poet.column.parameterizedBy(
                when (column.type) {
                    is Table.Column.Type.Array -> List::class.asTypeName()
                        .parameterizedBy(column.type.getTypeName())

                    else -> column.type.getTypeName()
                }
            ),
        ) {
            initializer(column)
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

    addInitializerBlock {
        this@toTypeSpecInternal.foreignKeys.forEach { foreignKey ->
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
