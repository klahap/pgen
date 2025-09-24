package io.github.klahap.pgen.model.config

import io.github.klahap.pgen.model.sql.KotlinEnumClass
import io.github.klahap.pgen.model.sql.SqlObjectName
import kotlinx.serialization.Serializable

@Serializable
data class EnumMapping(
    val sqlType: SqlObjectName,
    val enumClass: KotlinEnumClass,
)
