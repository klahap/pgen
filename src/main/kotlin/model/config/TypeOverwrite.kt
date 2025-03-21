package io.github.klahap.pgen.model.config

import io.github.klahap.pgen.model.sql.KotlinValueClass
import io.github.klahap.pgen.model.sql.SqlColumnName
import kotlinx.serialization.Serializable

@Serializable
data class TypeOverwrite(
    val sqlColumn: SqlColumnName,
    val valueClass: KotlinValueClass,
)