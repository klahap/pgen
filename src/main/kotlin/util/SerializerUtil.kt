package io.github.klahap.pgen.util

import io.github.klahap.pgen.model.sql.DbName
import io.github.klahap.pgen.model.sql.SchemaName
import io.github.klahap.pgen.model.sql.SqlObjectName
import io.github.klahap.pgen.model.sql.Table
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object ColumnTypeSerializer : KSerializer<Table.Column.Type> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Table.Column.Type")

    override fun serialize(encoder: Encoder, value: Table.Column.Type) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer can only be used with JSON")
        val jsonElement = when (value) {
            is Table.Column.Type.Primitive -> jsonEncoder.json.encodeToJsonElement(value)
            is Table.Column.Type.NonPrimitive -> jsonEncoder.json.encodeToJsonElement(value)
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): Table.Column.Type {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer can only be used with JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> Table.Column.Type.Primitive.valueOf(element.content)
            is JsonObject -> jsonDecoder.json.decodeFromJsonElement<Table.Column.Type.NonPrimitive>(element)
            else -> throw SerializationException("Invalid JSON for Column.Type")
        }
    }
}


object SqlObjectNameSerializer : KSerializer<SqlObjectName> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SqlObjectName")

    override fun serialize(encoder: Encoder, value: SqlObjectName) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer can only be used with JSON")
        val jsonElement = jsonEncoder.json.encodeToJsonElement(
            "${value.schema.dbName}.${value.schema.schemaName}.${value.name}"
        )
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): SqlObjectName {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer can only be used with JSON")
        val (db, schema, name) = jsonDecoder.decodeJsonElement().jsonPrimitive.content.split('.')
        return SqlObjectName(
            schema = SchemaName(
                dbName = DbName(db),
                schemaName = schema
            ),
            name = name
        )
    }
}