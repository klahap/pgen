package io.github.klahap.pgen.util

import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar
import io.github.klahap.pgen.model.sql.DbName
import io.github.klahap.pgen.model.sql.SchemaName
import io.github.klahap.pgen.model.sql.SqlObjectName
import io.github.klahap.pgen.model.sql.SqlStatementName
import io.github.klahap.pgen.model.sql.Table
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object ColumnTypeSerializer : KSerializer<Table.Column.Type> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        ColumnTypeSerializer::class.java.name,
        SerialKind.CONTEXTUAL
    ) {
        element<String>("primitive")
        element<Table.Column.Type.NonPrimitive>("nonPrimitive")
    }

    override fun serialize(encoder: Encoder, value: Table.Column.Type) {
        when (value) {
            is Table.Column.Type.Primitive ->
                encoder.encodeSerializableValue(Table.Column.Type.Primitive.serializer(), value)

            is Table.Column.Type.NonPrimitive ->
                encoder.encodeSerializableValue(Table.Column.Type.NonPrimitive.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): Table.Column.Type = when (decoder) {
        is JsonDecoder -> when (val node = decoder.decodeJsonElement()) {
            is JsonPrimitive -> Table.Column.Type.Primitive.valueOf(node.content)
            is JsonObject -> decoder.json.decodeFromJsonElement<Table.Column.Type.NonPrimitive>(node)
            else -> throw SerializationException("Invalid JSON for Column.Type")
        }

        is YamlInput -> {
            when (val node = decoder.node) {
                is YamlScalar -> Table.Column.Type.Primitive.valueOf(node.content)
                is YamlMap -> decoder.decodeSerializableValue(Table.Column.Type.NonPrimitive.serializer())
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
        encoder.encodeString("${value.schema.dbName}.${value.schema.schemaName}.${value.name}")
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