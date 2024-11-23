package io.github.klahap.pgen.model.sql

import com.squareup.kotlinpoet.ClassName
import io.github.klahap.pgen.util.codegen.CodeGenContext
import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.util.kotlinKeywords
import io.github.klahap.pgen.util.makeDifferent
import io.github.klahap.pgen.util.toCamelCase


@JvmInline
value class SchemaName(val name: String) {
    override fun toString() = name

    companion object {
        val PG_CATALOG = SchemaName("pg_catalog")
    }
}

sealed interface SqlObject {
    val name: SqlObjectName
}

sealed interface SqlObjectName {
    val schema: SchemaName
    val name: String
    val prettyName get() = name.toCamelCase(capitalized = true)

    context(CodeGenContext)
    val packageName
        get(): PackageName {
            val path = when (this) {
                is SqlEnumName -> "enumeration"
                is SqlTableName -> "table"
            }
            return PackageName("$rootPackageName.$path.${schema.name.makeDifferent(kotlinKeywords)}")
        }

    context(CodeGenContext)
    val typeName
        get() = ClassName(packageName.name, prettyName)

}

data class SqlTableName(
    override val schema: SchemaName,
    override val name: String,
) : SqlObjectName

data class SqlEnumName(
    override val schema: SchemaName,
    override val name: String,
) : SqlObjectName
