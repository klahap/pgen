package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import io.github.klahap.pgen.dsl.PackageName

data class CodeGenContext(
    val rootPackageName: PackageName,
    val createDirectoriesForRootPackageName: Boolean,
) {
    private val packageCustomColumn = PackageName("$rootPackageName.column_type")

    val getArrayColumnType
        get() = ClassName(packageCustomColumn.name, "getArrayColumnType")
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