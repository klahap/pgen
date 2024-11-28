package io.github.klahap.pgen.model.sql

import kotlinx.serialization.Serializable


@Serializable
data class PgenSpec(
    val tables: List<Table>,
    val enums: List<Enum>,
)
