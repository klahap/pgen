package io.github.klahap.pgen.model.oas

import com.squareup.kotlinpoet.ClassName
import io.github.klahap.pgen.model.config.Config
import io.github.klahap.pgen.model.sql.Enum

data class EnumOasData(
    val name: String,
    val items: List<String>,
    val sqlData: Enum,
) {
    val nameCapitalized = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    context(mapperConfig: Config.OasConfig.Mapper)
    fun getOasType() = ClassName(mapperConfig.packageOasModel, "${nameCapitalized}Dto")

    companion object {
        fun fromSqlData(data: Enum) = EnumOasData(
            name = data.name.prettyName,
            items = data.fields,
            sqlData = data,
        )
    }
}