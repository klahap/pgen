package io.github.klahap.pgen.util

import io.github.klahap.pgen.util.codegen.CodeGenContext

data class DefaultCodeFile(
    val relativePackageName: String,
    val fileName: String,
) {
    context(CodeGenContext)
    fun getContent(): String {
        val relativePath = relativePackageName.replace(".", "/") + "/" + fileName
        return this::class.java.getResourceAsStream("/default_code/$relativePath")!!
            .readAllBytes().decodeToString()
            .replaceFirst("package default_code", "package $rootPackageName")
            .replace("import default_code", "import $rootPackageName")
    }

    companion object {
        fun all() = setOf(
            DefaultCodeFile("column_type", "IntMultiRange.kt"),
            DefaultCodeFile("column_type", "IntRange.kt"),
            DefaultCodeFile("column_type", "MultiRange.kt"),
            DefaultCodeFile("column_type", "MultiRangeColumnType.kt"),
            DefaultCodeFile("column_type", "RangeColumnType.kt"),
            DefaultCodeFile("column_type", "UnconstrainedNumericColumnType.kt"),
            DefaultCodeFile("column_type", "UtilMultiRange.kt"),
            DefaultCodeFile("column_type", "UtilRange.kt"),
            DefaultCodeFile("column_type", "UtilEnum.kt"),
        )
    }
}
