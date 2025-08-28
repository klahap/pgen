package io.github.klahap.pgen.util

import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar
import io.github.klahap.pgen.model.sql.DbName
import io.github.klahap.pgen.model.sql.KotlinClassName
import io.github.klahap.pgen.model.sql.SchemaName
import io.github.klahap.pgen.model.sql.SqlColumnName
import io.github.klahap.pgen.model.sql.SqlObjectName
import io.github.klahap.pgen.model.sql.SqlStatementName
import io.github.klahap.pgen.model.sql.Column
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object ColumnTypeSerializer : KSerializer<Column.Type> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        ColumnTypeSerializer::class.java.name,
        SerialKind.CONTEXTUAL
    ) {
        element<String>("primitive")
        element<Column.Type.NonPrimitive>("nonPrimitive")
    }

    override fun serialize(encoder: Encoder, value: Column.Type) {
        when (value) {
            is Column.Type.Primitive ->
                encoder.encodeSerializableValue(Column.Type.Primitive.serializer(), value)

            is Column.Type.NonPrimitive ->
                encoder.encodeSerializableValue(Column.Type.NonPrimitive.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): Column.Type = when (decoder) {
        is JsonDecoder -> when (val node = decoder.decodeJsonElement()) {
            is JsonPrimitive -> Column.Type.Primitive.valueOf(node.content)
            is JsonObject -> decoder.json.decodeFromJsonElement<Column.Type.NonPrimitive>(node)
            else -> throw SerializationException("Invalid JSON for Column.Type")
        }

        is YamlInput -> {
            when (val node = decoder.node) {
                is YamlScalar -> Column.Type.Primitive.valueOf(node.content)
                is YamlMap -> decoder.decodeSerializableValue(Column.Type.NonPrimitive.serializer())
                else -> throw SerializationException("Invalid YAML for Column.Type")
            }
        }

        else -> throw SerializationException("This serializer can only be used with JSON or YAML")
    }
}

object SqlObjectNameSerializer : KSerializer<SqlObjectName> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        SqlObjectNameSerializer::class.java.name,
        SerialKind.CONTEXTUAL
    ) {
        element<String>("string")
    }

    override fun serialize(encoder: Encoder, value: SqlObjectName) {
        val valueStr = listOf(
            value.schema.dbName,
            value.schema.schemaName,
            value.name,
        ).joinToString(".")
        encoder.encodeString(valueStr)
    }

    override fun deserialize(decoder: Decoder): SqlObjectName {
        val (db, schema, name) = decoder.decodeString().split('.')
        return SqlObjectName(
            schema = SchemaName(
                dbName = DbName(db),
                schemaName = schema,
            ),
            name = name,
        )
    }
}


object SqlColumnNameSerializer : KSerializer<SqlColumnName> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        SqlColumnNameSerializer::class.java.name,
        SerialKind.CONTEXTUAL
    ) {
        element<String>("string")
    }

    override fun serialize(encoder: Encoder, value: SqlColumnName) {
        val valueStr = listOf(
            value.tableName.schema.dbName,
            value.tableName.schema.schemaName,
            value.tableName.name,
            value.name,
        ).joinToString(".")
        encoder.encodeString(valueStr)
    }

    override fun deserialize(decoder: Decoder): SqlColumnName {
        val (db, schema, table, name) = decoder.decodeString().split('.')
        val tableName = SqlObjectName(
            schema = SchemaName(
                dbName = DbName(db),
                schemaName = schema,
            ),
            name = table,
        )
        return SqlColumnName(tableName = tableName, name = name)
    }
}


object KotlinClassNameSerializer : KSerializer<KotlinClassName> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        KotlinClassName::class.java.name,
        SerialKind.CONTEXTUAL
    ) {
        element<String>("string")
    }

    override fun serialize(encoder: Encoder, value: KotlinClassName) {
        val valueStr = listOf(
            value.packageName,
            value.className,
        ).joinToString(".")
        encoder.encodeString(valueStr)
    }

    override fun deserialize(decoder: Decoder): KotlinClassName {
        val str = decoder.decodeString()
        val className = KotlinClassName(
            packageName = str.substringBeforeLast('.'),
            className = str.substringAfterLast('.'),
        )
        return className
    }
}


object SqlStatementNameSerializer : KSerializer<SqlStatementName> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        SqlStatementNameSerializer::class.java.name,
        SerialKind.CONTEXTUAL
    ) {
        element<String>("string")
    }

    override fun serialize(encoder: Encoder, value: SqlStatementName) {
        encoder.encodeString("${value.dbName}.${value.name}")
    }

    override fun deserialize(decoder: Decoder): SqlStatementName {
        val (db, name) = decoder.decodeString().split('.')
        return SqlStatementName(
            dbName = DbName(db),
            name = name,
        )
    }
}