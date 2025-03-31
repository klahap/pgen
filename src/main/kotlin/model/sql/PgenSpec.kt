package io.github.klahap.pgen.model.sql

import io.github.klahap.pgen.model.config.TypeMapping
import io.github.klahap.pgen.model.config.TypeOverwrite
import kotlinx.serialization.Serializable


@Serializable
data class PgenSpec(
    val tables: List<Table>,
    val enums: List<Enum>,
    val compositeTypes: List<CompositeType>,
    val statements: List<Statement>,
    val typeMappings: List<TypeMapping>,
    val typeOverwrites: List<TypeOverwrite>,
) {
    val domains
        get() = tables
            .flatMap { it.columns.map(Column::type) }
            .filterIsInstance<Column.Type.NonPrimitive.Domain>()
}
