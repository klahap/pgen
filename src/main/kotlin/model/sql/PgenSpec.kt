package io.github.klahap.pgen.model.sql

import io.github.klahap.pgen.model.config.TypeMapping
import kotlinx.serialization.Serializable


@Serializable
data class PgenSpec(
    val tables: List<Table>,
    val enums: List<Enum>,
    val statements: List<Statement>,
    val typeMappings: List<TypeMapping>,
) {
    val domains
        get() = tables
            .flatMap { it.columns.map(Table.Column::type) }
            .filterIsInstance<Table.Column.Type.NonPrimitive.Domain>()
}
