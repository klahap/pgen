package io.github.klahap.pgen.model.oas

import io.github.klahap.pgen.model.sql.Enum

data class EnumOasData(
    val name: String,
    val items: List<String>,
) {
    val nameCapitalized = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    companion object {
        fun fromSqlData(data: Enum) = EnumOasData(
            name = data.name.prettyName,
            items = data.fields,
        )
    }
}