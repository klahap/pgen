package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.model.config.TypeMapping
import io.github.klahap.pgen.model.sql.SqlObjectName

data class CodeGenContext(
    val rootPackageName: PackageName,
    val createDirectoriesForRootPackageName: Boolean,
    val typeMappings: Map<SqlObjectName, TypeName>,
) {
    private val packageCustomColumn = PackageName("$rootPackageName.column_type")

    val getArrayColumnType
        get() = ClassName(packageCustomColumn.name, "getArrayColumnType")
    val domainType
        get() = ClassName(packageCustomColumn.name, "domainType")
    val domainTypeColumn
        get() = ClassName(packageCustomColumn.name, "DomainTypeColumn")
    val customEnumerationArray
        get() = ClassName(packageCustomColumn.name, "customEnumerationArray")
    val typeNameMultiRange
        get() = ClassName(packageCustomColumn.name, "MultiRange")
    val defaultJsonColumnType
        get() = ClassName(packageCustomColumn.name, "DefaultJsonColumnType")
    val typeNameInt4RangeColumnType
        get() = ClassName(packageCustomColumn.name, "Int4RangeColumnType")
    val typeNameInt8RangeColumnType
        get() = ClassName(packageCustomColumn.name, "Int8RangeColumnType")
    val typeNameInt4MultiRangeColumnType
        get() = ClassName(packageCustomColumn.name, "Int4MultiRangeColumnType")
    val typeNameInt8MultiRangeColumnType
        get() = ClassName(packageCustomColumn.name, "Int8MultiRangeColumnType")
    val typeNameUnconstrainedNumericColumnType
        get() = ClassName(packageCustomColumn.name, "UnconstrainedNumericColumnType")

    val typeNamePgEnum get() = ClassName(packageCustomColumn.name, "PgEnum")
    val typeNameGetPgEnumByLabel get() = ClassName(packageCustomColumn.name, "getPgEnumByLabel")
}