package default_code.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonBuilder
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