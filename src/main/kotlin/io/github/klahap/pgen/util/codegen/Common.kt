package io.github.klahap.pgen.util.codegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asTypeName
import io.github.klahap.pgen.dsl.PackageName
import io.github.klahap.pgen.dsl.fileSpec
import io.github.klahap.pgen.model.config.Config
import io.github.klahap.pgen.model.oas.EnumOasData
import io.github.klahap.pgen.model.oas.TableOasData
import io.github.klahap.pgen.model.sql.Enum
import io.github.klahap.pgen.model.sql.SqlObject
import io.github.klahap.pgen.model.sql.Statement
import io.github.klahap.pgen.model.sql.Table
import io.github.klahap.pgen.model.sql.Column.Type.NonPrimitive.Domain
import io.github.klahap.pgen.model.sql.CompositeType
import io.github.klahap.pgen.service.DirectorySyncService
import io.github.klahap.pgen.util.StaticCodeFile
import io.github.klahap.pgen.util.codegen.oas.addEnumMapper
import io.github.klahap.pgen.util.codegen.oas.addTableMapper
import io.github.klahap.pgen.util.codegen.oas.addTableService
import java.time.OffsetDateTime

object Poet {
    val json = ClassName("kotlinx.serialization.json", "Json")
    val jsonElement = ClassName("kotlinx.serialization.json", "JsonElement")

    val duration = ClassName("kotlinx.datetime", "Duration")
    val localTime = ClassName("kotlinx.datetime", "LocalTime")
    val localDate = ClassName("kotlinx.datetime", "LocalDate")
    val offsetDateTime = OffsetDateTime::class.asTypeName()

    val flowSingle = ClassName("kotlinx.coroutines.flow", "single")
    val flow = ClassName("kotlinx.coroutines.flow", "Flow")
    val generateChannelFlow = ClassName("kotlinx.coroutines.flow", "channelFlow")
    val trySendBlocking = ClassName("kotlinx.coroutines.channels", "trySendBlocking")

    val PGobject = ClassName("org.postgresql.util", "PGobject")

    val codecRegistrar = ClassName("io.r2dbc.postgresql.extension", "CodecRegistrar")

    private val packageExposed = PackageName("org.jetbrains.exposed.v1")
    private val packageExposedCore = packageExposed.plus("core")
    private val packageExposedJson = packageExposed.plus("json")
    private val packageExposedDatetime = packageExposed.plus("datetime")

    val date = packageExposedDatetime.className("date")
    val time = packageExposedDatetime.className("time")
    val timestamp = packageExposedDatetime.className("timestamp")
    val timestampWithTimeZone = packageExposedDatetime.className("timestampWithTimeZone")
    val defaultExpTimestamp = packageExposedDatetime.className("CurrentTimestamp")
    val defaultExpTimestampZ = packageExposedDatetime.className("CurrentTimestampWithTimeZone")
    val kotlinLocalDateColumnType = packageExposedDatetime.className("KotlinLocalDateColumnType")
    val kotlinDurationColumnType = packageExposedDatetime.className("KotlinDurationColumnType")
    val kotlinLocalTimeColumnType = packageExposedDatetime.className("KotlinLocalTimeColumnType")
    val kotlinInstantColumnType = packageExposedDatetime.className("KotlinInstantColumnType")
    val kotlinOffsetDateTimeColumnType = packageExposedDatetime.className("KotlinOffsetDateTimeColumnType")

    val customFunction = packageExposedCore.className("CustomFunction")
    val table = packageExposedCore.className("Table")
    val transaction = packageExposedCore.className("Transaction")
    val columnType = packageExposedCore.className("ColumnType")
    val primaryKey = packageExposedCore.className("Table", "PrimaryKey")
    val column = packageExposedCore.className("Column")
    val alias = packageExposedCore.className("Alias")
    val resultRow = packageExposedCore.className("ResultRow")
    val uuidColumnType = packageExposedCore.className("UUIDColumnType")
    val enumerationColumnType = packageExposedCore.className("EnumerationColumnType")
    val decimalColumnType = packageExposedCore.className("DecimalColumnType")
    val longColumnType = packageExposedCore.className("LongColumnType")
    val booleanColumnType = packageExposedCore.className("BooleanColumnType")
    val binaryColumnType = packageExposedCore.className("BinaryColumnType")
    val textColumnType = packageExposedCore.className("TextColumnType")
    val integerColumnType = packageExposedCore.className("IntegerColumnType")
    val floatColumnType = packageExposedCore.className("FloatColumnType")
    val doubleColumnType = packageExposedCore.className("DoubleColumnType")
    val shortColumnType = packageExposedCore.className("ShortColumnType")
    val updateBuilder = packageExposedCore.plus("statements").className("UpdateBuilder")

    val jsonColumn = packageExposedJson.className("json")
    val option = ClassName("io.github.goquati.kotlin.util", "Option")
    val optionTakeSome = ClassName("io.github.goquati.kotlin.util", "takeSome")
    val optionMap = ClassName("io.github.goquati.kotlin.util", "map")
    val dbR2dbc = ClassName("org.jetbrains.exposed.v1.r2dbc", "R2dbcDatabase")
    val quatiException = ClassName("io.github.goquati.kotlin.util", "QuatiException")
    val flowSingleOrNull = ClassName("kotlinx.coroutines.flow", "singleOrNull")
    val r2dbcUpdateReturning = ClassName("org.jetbrains.exposed.v1.r2dbc", "updateReturning")
    val r2dbcInsertReturning = ClassName("org.jetbrains.exposed.v1.r2dbc", "insertReturning")
    val r2dbcSelectAll = ClassName("org.jetbrains.exposed.v1.r2dbc", "selectAll")
    val r2dbcDeleteWhere = ClassName("org.jetbrains.exposed.v1.r2dbc", "deleteWhere")
    val r2dbcSuspendTransaction = ClassName("org.jetbrains.exposed.v1.r2dbc.transactions", "suspendTransaction")
    val eq = ClassName("org.jetbrains.exposed.v1.core.SqlExpressionBuilder", "eq")
    val opBoolean = ClassName("org.jetbrains.exposed.v1.core", "Op").parameterizedBy(Boolean::class.asTypeName())
    val sqlExpressionBuilder = ClassName("org.jetbrains.exposed.v1.core", "SqlExpressionBuilder")
    val channelFlow = ClassName("kotlinx.coroutines.flow", "channelFlow")
    val flowMap = ClassName("kotlinx.coroutines.flow", "map")
}

context(c: CodeGenContext)
private fun SqlObject.toTypeSpec() = when (this) {
    is Enum -> toTypeSpecInternal()
    is Table -> toTypeSpecInternal()
    is Domain -> toTypeSpecInternal()
    is CompositeType -> toTypeSpecInternal()
}

context(c: CodeGenContext)
fun FileSpec.Builder.add(obj: SqlObject) {
    val spec = obj.toTypeSpec()
    addType(spec)
}

context(c: CodeGenContext)
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

context(c: CodeGenContext)
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

context(c: CodeGenContext, mapperConfig: Config.OasConfig.Mapper)
fun DirectorySyncService.sync(
    obj: EnumOasData,
    block: FileSpec.Builder.() -> Unit = {},
) {
    val fileName = "${obj.nameCapitalized}.kt"
    val packageName = c.poet.packageMapper
    sync(
        relativePath = packageName.toRelativePath() + "/$fileName",
        content = fileSpec(
            packageName = packageName,
            name = fileName,
            block = {
                addEnumMapper(obj)
                block()
            }
        )
    )
}

context(c: CodeGenContext, mapperConfig: Config.OasConfig.Mapper)
fun DirectorySyncService.sync(
    obj: TableOasData,
    block: FileSpec.Builder.() -> Unit = {},
) {
    run {
        val fileName = "${obj.nameCapitalized}.kt"
        val packageName = c.poet.packageMapper
        sync(
            relativePath = packageName.toRelativePath() + "/$fileName",
            content = fileSpec(
                packageName = packageName,
                name = fileName,
                block = {
                    addTableMapper(obj)
                    block()
                }
            )
        )
    }
    if (c.connectionType == Config.ConnectionType.R2DBC) {
        val fileName = obj.getOasServiceName() + ".kt"
        val packageName = c.poet.packageService
        sync(
            relativePath = packageName.toRelativePath() + "/$fileName",
            content = fileSpec(
                packageName = packageName,
                name = fileName,
                block = {
                    addTableService(obj)
                    block()
                }
            )
        )
    }
}

context(c: CodeGenContext)
fun DirectorySyncService.syncCodecs(
    objs: Collection<Enum>,
) {
    if (c.connectionType != Config.ConnectionType.R2DBC) return
    val fileName = "R2dbcCodecs.kt"
    val packageName = c.poet.rootPackageName
    sync(
        relativePath = fileName,
        content = fileSpec(
            packageName = packageName,
            name = fileName,
            block = { createCodecCollection(objs) }
        )
    )
}

context(c: CodeGenContext)
fun DirectorySyncService.sync(codeFile: StaticCodeFile) {
    sync(
        relativePath = codeFile.relativePath,
        content = codeFile.getContent()
    )
}

context(c: CodeGenContext)
fun PackageName.toRelativePath() = if (c.createDirectoriesForRootPackageName)
    name.replace(".", "/")
else
    name.removePrefix(c.poet.rootPackageName.name).trimStart('.').replace(".", "/")
