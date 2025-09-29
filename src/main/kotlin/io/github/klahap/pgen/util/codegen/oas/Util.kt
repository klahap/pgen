package io.github.klahap.pgen.util.codegen.oas

import io.github.klahap.pgen.model.config.Config

internal const val REF = "${'$'}ref"
internal const val INDENT = "  "

internal enum class DtoType { GET, CREATE, UPDATE }

internal val Config.OasConfig.CRUD.requiredDtos
    get() = when (this) {
        Config.OasConfig.CRUD.CREATE -> setOf(DtoType.CREATE, DtoType.GET)
        Config.OasConfig.CRUD.READ -> setOf(DtoType.GET)
        Config.OasConfig.CRUD.UPDATE -> setOf(DtoType.UPDATE, DtoType.GET)
        Config.OasConfig.CRUD.DELETE -> setOf()
        Config.OasConfig.CRUD.READ_ALL -> setOf(DtoType.GET)
    }
private fun String.indent(level: Int) = INDENT.repeat(level) + this
private fun List<String>.indent(level: Int) = map { it.indent(level) }


internal fun yaml(level: Int, block: YamlBuilder.() -> Unit) = YamlBuilder(level = level).apply(block).build()

internal class YamlBuilder(private val level: Int) {
    private val lines = mutableListOf<String>()

    fun indent(line: String? = null, block: YamlBuilder.() -> Unit): Unit = YamlBuilder(level = level + 1)
        .apply(block)
        .let {
            if (line != null) add(line)
            lines.addAll(it.lines)
        }

    fun add(line: String): Unit = run { lines.add(line.indent(level)) }
    fun add(vararg lines: String): Unit = add(lines.toList())
    fun add(lines: List<String>): Unit = run { this.lines.addAll(lines.indent(level)) }
    fun build() = lines.joinToString("\n")
}
