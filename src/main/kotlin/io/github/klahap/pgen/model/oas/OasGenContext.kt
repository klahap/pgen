package io.github.klahap.pgen.model.oas

data class OasGenContext(
    val pathPrefix: String,
    val meta: MetaOasData,
    val oasCommonName: String,
)
