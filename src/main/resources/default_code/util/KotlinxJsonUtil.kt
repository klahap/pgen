package default_code.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlin.reflect.KClass


fun JsonBuilder.serializersModule(block: SerializersModuleBuilder.() -> Unit) {
    serializersModule = SerializersModule(block)
}

fun <T : Any> SerializersModuleBuilder.add(serializer: QuatiKotlinxStringSerializer<T>) {
    contextual(serializer.clazz, serializer)
}

fun SerializersModuleBuilder.add(serializers: Iterable<QuatiKotlinxStringSerializer<*>>) {
    serializers.forEach { add(it) }
}

interface QuatiKotlinxStringSerializer<T : Any> : KSerializer<T> {
    val clazz: KClass<T>
}

fun PgenErrorDetails.toJson() = buildJsonObject {
    put("code", code.let(::JsonPrimitive))
    put("columnName", columnName.let(::JsonPrimitive))
    put("constraintName", constraintName.let(::JsonPrimitive))
    put("dataTypeName", dataTypeName.let(::JsonPrimitive))
    put("detail", detail.let(::JsonPrimitive))
    put("file", file.let(::JsonPrimitive))
    put("hint", hint.let(::JsonPrimitive))
    put("internalPosition", internalPosition.let(::JsonPrimitive))
    put("internalQuery", internalQuery.let(::JsonPrimitive))
    put("line", line.let(::JsonPrimitive))
    put("message", message.let(::JsonPrimitive))
    put("position", position.let(::JsonPrimitive))
    put("routine", routine.let(::JsonPrimitive))
    put("schemaName", schemaName.let(::JsonPrimitive))
    put("severityLocalized", severityLocalized.let(::JsonPrimitive))
    put("severityNonLocalized", severityNonLocalized.let(::JsonPrimitive))
    put("tableName", tableName.let(::JsonPrimitive))
    put("where", where.let(::JsonPrimitive))
}
