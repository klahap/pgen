package io.github.klahap.pgen.model.oas

data class TableFieldOasData(
    val name: String,
    val nullable: Boolean,
    val ignoreAtCreate: Boolean,
    val ignoreAtUpdate: Boolean,
    val type: TableFieldTypeOasData,
)