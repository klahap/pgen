package io.github.klahap.pgen.util

import io.github.klahap.pgen.util.codegen.CodeGenContext
import java.io.File

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
        private fun getResources(path: List<String>): Sequence<List<String>> {
            val pathStr = path.joinToString("/")
            val resource = javaClass.classLoader.getResource(pathStr) ?: error("Resource not found: $pathStr")
            val childs = File(resource.toURI()).list()?.toList() ?: return sequenceOf(path)
            return childs.asSequence().flatMap { child ->
                getResources(path + listOf(child))
            }
        }

        fun all() = getResources(listOf("default_code"))
            .map { DefaultCodeFile(it.drop(1).dropLast(1), it.last()) }.toSet()
    }
}
