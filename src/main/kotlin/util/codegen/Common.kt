package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.dsl.fileSpec
import io.github.klahap.pgen.model.sql.Enum
import io.github.klahap.pgen.model.sql.SqlObject
import io.github.klahap.pgen.model.sql.Statement
import io.github.klahap.pgen.model.sql.Table
import io.github.klahap.pgen.model.sql.Table.Column.Type.NonPrimitive.Domain
import io.github.klahap.pgen.service.DirectorySyncService
import io.github.klahap.pgen.util.DefaultCodeFile
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

    val defaultExpTimestamp = ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "CurrentTimestamp")
    val defaultExpTimestampZ = ClassName("org.jetbrains.exposed.sql.kotlin.datetime", "CurrentTimestampWithTimeZone")
    val customFunction = ClassName("org.jetbrains.exposed.sql", "CustomFunction")
    val uuidColumnType = ClassName("org.jetbrains.exposed.sql", "UUIDColumnType")

    val flowSingle = ClassName("kotlinx.coroutines.flow", "single")
    val flow = ClassName("kotlinx.coroutines.flow", "Flow")
    val generateChannelFlow = ClassName("kotlinx.coroutines.flow", "channelFlow")
    val trySendBlocking = ClassName("kotlinx.coroutines.channels", "trySendBlocking")
    val transaction = ClassName("org.jetbrains.exposed.sql", "Transaction")
}

context(CodeGenContext)
fun SqlObject.toTypeSpec() = when (this) {
    is Enum -> toTypeSpecInternal()
    is Table -> toTypeSpecInternal()
    is Domain -> toTypeSpecInternal()
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
fun DirectorySyncService.sync(
    obj: Collection<Statement>,
    block: FileSpec.Builder.() -> Unit = {},
) {
    val fileName = "Statements.kt"
    val packageName = obj.packageName
    sync(
        relativePath = packageName.toRelativePath() + "/$fileName",
        content = fileSpec(
            packageName = packageName,
            name = fileName,
            block = {
                addStatements(obj)
                block()
            }
        )
    )
}

context(CodeGenContext)
fun DirectorySyncService.sync(codeFile: DefaultCodeFile) {
    sync(
        relativePath = codeFile.relativePath,
        content = codeFile.getContent()
    )
}

context(CodeGenContext)
fun PackageName.toRelativePath() = if (createDirectoriesForRootPackageName)
    name.replace(".", "/")
else
    name.removePrefix(rootPackageName.name).trimStart('.').replace(".", "/")
