package io.github.klahap.pgen.util

import io.github.klahap.pgen.util.codegen.CodeGenContext

data class DefaultCodeFile(
    private val relativePackageNames: List<String>,
    val fileName: String,
) {
    val relativePath get() = (relativePackageNames + listOf(fileName)).joinToString("/")

    context(CodeGenContext)
    fun getContent(): String = javaClass.getResourceAsStream("/default_code/$relativePath")!!
        .readAllBytes().decodeToString()
        .replaceFirst("package default_code", "package $rootPackageName")
        .replace("import default_code", "import $rootPackageName")

    companion object {
        fun all() = setOf(
            DefaultCodeFile(listOf("column_type"), "DefaultJsonColumnType.kt"),
            DefaultCodeFile(listOf("column_type"), "DomainColumnType.kt"),
            DefaultCodeFile(listOf("column_type"), "IntMultiRange.kt"),
            DefaultCodeFile(listOf("column_type"), "IntRange.kt"),
            DefaultCodeFile(listOf("column_type"), "MultiRange.kt"),
            DefaultCodeFile(listOf("column_type"), "MultiRangeColumnType.kt"),
            DefaultCodeFile(listOf("column_type"), "RangeColumnType.kt"),
            DefaultCodeFile(listOf("column_type"), "UnconstrainedNumericColumnType.kt"),
            DefaultCodeFile(listOf("column_type"), "Util.kt"),
            DefaultCodeFile(listOf("util"), "BatchUpdateStatementDsl.kt"),
        )
    }
}
