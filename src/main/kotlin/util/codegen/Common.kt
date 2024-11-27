package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.dsl.fileSpec
import io.github.klahap.pgen.model.sql.Enum
import io.github.klahap.pgen.model.sql.SqlObject
import io.github.klahap.pgen.model.sql.Table
import io.github.klahap.pgen.service.DirectorySyncService
import java.time.OffsetDateTime

object Poet {
    val table = ClassName("org.jetbrains.exposed.sql", "Table")
    val primaryKey = ClassName("org.jetbrains.exposed.sql", "Table", "PrimaryKey")
    val column = ClassName("org.jetbrains.exposed.sql", "Column")
    val date = ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "date")
    val time = ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "time")
    val timestamp = ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "timestamp")
    val timestampWithTimeZone = ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "timestampWithTimeZone")
    val jsonColumn = ClassName("org.jetbrains.exposed.sql.json", "json")

    val json = ClassName("kotlinx.serialization.json", "Json")
    val jsonElement = ClassName("kotlinx.serialization.json", "JsonElement")

    val instant = ClassName("kotlinx.datetime", "Instant")
    val duration = ClassName("kotlinx.datetime", "Duration")
    val localTime = ClassName("kotlinx.datetime", "LocalTime")
    val localDate = ClassName("kotlinx.datetime", "LocalDate")
    val offsetDateTime = OffsetDateTime::class.asTypeName()

    private const val PACKAGE_CUSTOM_COLUMN = "io.github.klahap.pgenlib.column_type"
    val multiRange = ClassName(PACKAGE_CUSTOM_COLUMN, "MultiRange")
    val int4RangeColumnType = ClassName(PACKAGE_CUSTOM_COLUMN, "Int4RangeColumnType")
    val int8RangeColumnType = ClassName(PACKAGE_CUSTOM_COLUMN, "Int8RangeColumnType")
    val int4MultiRangeColumnType = ClassName(PACKAGE_CUSTOM_COLUMN, "Int4MultiRangeColumnType")
    val int8MultiRangeColumnType = ClassName(PACKAGE_CUSTOM_COLUMN, "Int8MultiRangeColumnType")
    val unconstrainedNumericColumnType = ClassName(PACKAGE_CUSTOM_COLUMN, "UnconstrainedNumericColumnType")

    val pgEnum = ClassName(PACKAGE_CUSTOM_COLUMN, "PgEnum")
    val getPgEnumByLabel = ClassName(PACKAGE_CUSTOM_COLUMN, "getPgEnumByLabel")
}

context(CodeGenContext)
fun SqlObject.toTypeSpec() = when (this) {
    is Enum -> toTypeSpecInternal()
    is Table -> toTypeSpecInternal()
}

context(CodeGenContext)
fun FileSpec.Builder.add(obj: SqlObject) {
    addType(obj.toTypeSpec())
}

context(CodeGenContext)
fun DirectorySyncService.sync(
    obj: SqlObject,
    block: FileSpec.Builder.() -> Unit = {},
) {
    val fileName = "${obj.name.prettyName}.kt"
    sync(
        relativePath = obj.name.packageName.toRelativePath() + "/$fileName",
        content = fileSpec(
            packageName = obj.name.packageName,
            name = fileName,
            block = {
                add(obj)
                block()
            }
        )
    )
}

context(CodeGenContext)
fun PackageName.toRelativePath() = if (createDirectoriesForRootPackageName)
    name.replace(".", "/")
else
    name.removePrefix(rootPackageName.name).trimStart('.').replace(".", "/")
