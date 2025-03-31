package io.github.klahap.pgen.model.sql

import kotlinx.serialization.Serializable

@Serializable
data class CompositeType(
    override val name: SqlObjectName,
    val columns: List<Column>,
) : SqlObject {
    val type get() = Column.Type.NonPrimitive.Composite(name)
}