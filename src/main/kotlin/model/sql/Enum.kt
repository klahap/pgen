package io.github.klahap.pgen.model.sql

import kotlinx.serialization.Serializable

@Serializable
data class Enum(
    override val name: SqlObjectName,
    val fields: List<String>,
) : SqlObject, Comparable<Enum> {
    override fun compareTo(other: Enum) = name.compareTo(other.name)
}
