package io.github.klahap.pgen.model.config

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import io.github.klahap.pgen.model.sql.SqlObjectName
import kotlinx.serialization.Serializable

@Serializable
data class TypeMapping(
    val sqlType: SqlObjectName,
    val clazz: String,
) {
    val clazzClassName: TypeName get() = ClassName(clazz.substringBeforeLast('.'), clazz.substringAfterLast('.'))
}
