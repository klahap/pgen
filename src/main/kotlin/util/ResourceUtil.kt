package io.github.klahap.pgen.util

import io.github.klahap.pgen.model.config.Config
import io.github.klahap.pgen.util.codegen.CodeGenContext

data class DefaultCodeFile(
    private val relativePackageNames: List<String>,
    val fileName: String,
) {
    val relativePath get() = (relativePackageNames + listOf(fileName)).joinToString("/")

    context(CodeGenContext)
    fun getContent(): String = javaClass.getResourceAsStream("/default_code/$relativePath")!!
        .readAllBytes().decodeToString()
        .replaceFirst("package default_code", "package ${poet.rootPackageName}")
        .replace("import default_code", "import ${poet.rootPackageName}")

    companion object {
        suspend fun SequenceScope<DefaultCodeFile>.yield(relativePackageNames: List<String>, fileName: String) {
            yield(DefaultCodeFile(relativePackageNames = relativePackageNames, fileName = fileName))
        }

        fun all(connectionType: Config.ConnectionType) = sequence {
            yield(listOf("column_type"), "DefaultJsonColumnType.kt")
            yield(listOf("column_type"), "DomainColumnType.kt")
            yield(listOf("column_type"), "PgStructUtil.kt")
            yield(listOf("column_type"), "UnconstrainedNumericColumnType.kt")
            yield(listOf("column_type"), "Util.kt")
            yield(listOf("column_type"), "PgVectorColumnType.kt")
            if (connectionType == Config.ConnectionType.JDBC) {
                yield(listOf("column_type"), "IntMultiRange.kt")
                yield(listOf("column_type"), "IntRange.kt")
                yield(listOf("column_type"), "MultiRange.kt")
                yield(listOf("column_type"), "MultiRangeColumnType.kt")
                yield(listOf("column_type"), "RangeColumnType.kt")
            }
            yield(listOf("util"), "BatchUpdateStatementDsl.kt")
            yield(listOf("util"), "Dsl.kt")
            yield(listOf("util"), "ConnectionConfig.kt")
            yield(listOf("util"), "Exception.kt")
            when (connectionType) {
                Config.ConnectionType.JDBC -> yield(listOf("util"), "JdbcDsl.kt")
                Config.ConnectionType.R2DBC -> yield(listOf("util"), "R2dbcDsl.kt")
            }
            when (connectionType) {
                Config.ConnectionType.JDBC -> yield(listOf("column_type"), "UtilJdbc.kt")
                Config.ConnectionType.R2DBC -> yield(listOf("column_type"), "UtilR2dbc.kt")
            }
        }
    }
}
