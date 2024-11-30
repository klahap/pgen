package io.github.klahap.pgen.model.sql

import com.squareup.kotlinpoet.ClassName
import io.github.klahap.pgen.util.codegen.CodeGenContext
import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.util.SqlObjectNameSerializer
import io.github.klahap.pgen.util.kotlinKeywords
import io.github.klahap.pgen.util.makeDifferent
import io.github.klahap.pgen.util.toCamelCase
import kotlinx.serialization.Serializable

@JvmInline
value class DbName(val name: String) : Comparable<DbName> {
    override fun compareTo(other: DbName) = name.compareTo(other.name)
    override fun toString() = name

    fun toSchema(schemaName: String) = SchemaName(dbName = this, schemaName = schemaName)
    val schemaPgCatalog get() = toSchema(schemaName = "pg_catalog")
}

data class SchemaName(val dbName: DbName, val schemaName: String) : Comparable<SchemaName> {
    override fun toString(): String {
        return super.toString()
    }
    override fun compareTo(other: SchemaName) =
        dbName.compareTo(other.dbName).takeIf { it != 0 }
            ?: schemaName.compareTo(other.schemaName)
}

sealed interface SqlObject {
    val name: SqlObjectName
}

@Serializable(with = SqlObjectNameSerializer::class)
data class SqlObjectName(
    val schema: SchemaName,
    val name: String,
) : Comparable<SqlObjectName> {

    val prettyName get() = name.toCamelCase(capitalized = true)

    context(CodeGenContext)
    val packageName
        get()= PackageName("$rootPackageName.db.${schema.dbName}.${schema.schemaName.makeDifferent(kotlinKeywords)}")

    context(CodeGenContext)
    val typeName
        get() = ClassName(packageName.name, prettyName)

    override fun compareTo(other: SqlObjectName): Int =
        schema.compareTo(other.schema).takeIf { it != 0 }
            ?: name.compareTo(other.name)
}
