package io.github.klahap.pgen.model.config

import io.github.klahap.pgen.model.sql.KotlinClassName
import io.github.klahap.pgen.model.sql.SqlColumnName
import kotlinx.serialization.Serializable

@Serializable
data class TypeOverwrite(
    val sqlColumn: SqlColumnName,
    val clazz: KotlinClassName,
)