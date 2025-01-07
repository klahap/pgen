package default_code.column_type

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.jetbrains.exposed.sql.json.JsonColumnType

class DefaultJsonColumnType(json: Json = Json) : JsonColumnType<JsonElement>(
    { json.encodeToString(serializer<JsonElement>(), it) },
    { json.decodeFromString(serializer<JsonElement>(), it) }
)
