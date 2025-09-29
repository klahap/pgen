package io.github.klahap.pgen.model.oas

import io.github.klahap.pgen.model.config.Config
import io.github.klahap.pgen.model.sql.Table

private infix fun <T> Set<T>.anyIn(other: Set<T>) = this.intersect(other).isNotEmpty()

data class TableOasData(
    private val name: String,
    val fields: List<TableFieldOasData>,
    val endpoints: Set<Config.OasConfig.CRUD>,
) {
    val idFormat = fields.singleOrNull { it.name == "id" }?.type?.let { it as? TableFieldTypeOasData.Type }
        .also { if (it == null) error("'id' field not found for table $name") }
        ?.format
    val namePretty = name.replaceFirstChar { if (it.isTitleCase()) it.lowercase() else it.toString() }
    val path = namePretty
    val tag = namePretty
    val nameCapitalized = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val fieldsAtCreate get() = fields.filterNot { it.ignoreAtCreate }
    val fieldsAtUpdate get() = fields.filterNot { it.ignoreAtUpdate }

    companion object {
        fun fromData(data: Table, config: Config.OasConfig.Table): TableOasData {
            val fields = data.columns.mapNotNull { column ->
                val possibleNames = setOf(column.name.value, column.name.pretty)
                if (possibleNames anyIn config.ignoreFields) return@mapNotNull null
                TableFieldOasData(
                    name = column.prettyName,
                    nullable = column.isNullable,
                    ignoreAtCreate = possibleNames anyIn config.ignoreFieldsAtCreate,
                    ignoreAtUpdate = possibleNames anyIn config.ignoreFieldsAtUpdate,
                    type = TableFieldTypeOasData.fromData(column.type)
                )
            }

            return TableOasData(
                name = data.name.prettyName,
                fields = fields,
                endpoints = Config.OasConfig.CRUD.entries.filter { it !in config.ignoreMethods }.toSet()
            )
        }
    }
}