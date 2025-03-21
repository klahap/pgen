package io.github.klahap.pgen.model.config

import io.github.klahap.pgen.model.sql.KotlinValueClass
import io.github.klahap.pgen.model.sql.SqlObjectName
import kotlinx.serialization.Serializable

@Serializable
data class TypeMapping(
    val sqlType: SqlObjectName,
    val valueClass: KotlinValueClass,
)
