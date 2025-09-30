package io.github.klahap.pgen.model.oas

import io.github.klahap.pgen.model.sql.Column

data class TableFieldOasData(
    val name: String,
    val nullable: Boolean,
    val ignoreAtCreate: Boolean,
    val ignoreAtUpdate: Boolean,
    val type: TableFieldTypeOasData,
    val sqlData: Column,
)