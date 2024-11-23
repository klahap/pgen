package io.github.klahap.pgen.model.sql

data class Enum(
    override val name: SqlEnumName,
    val fields: List<String>,
) : SqlObject
