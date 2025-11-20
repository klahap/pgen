package default_code.column_type

import org.jetbrains.exposed.v1.core.ColumnType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class DomainColumnType<T : Any>(
    kClass: KClass<T>,
    private val sqlType: String,
    private val builder: (Any) -> T,
) : ColumnType<T>() {
    private val getter: KProperty1<T, Any>

    init {
        require(kClass.isValue) { "$kClass is not a value class" }

        val paramName = kClass.primaryConstructor
            .let { it ?: error("$kClass has no primary constructor") }
            .parameters.singleOrNull()?.name
            ?: error("$kClass does not have exactly one constructor parameter")

        getter = kClass.memberProperties
            .singleOrNull { it.name == paramName }
            .let { it ?: error("property '$paramName' not found in $kClass") }
            .also { require(!it.returnType.isMarkedNullable) { "property '$paramName' of $kClass is nullable" } }
            .let { @Suppress("UNCHECKED_CAST") (it as KProperty1<T, Any>) }
    }

    override fun sqlType(): String = sqlType
    override fun notNullValueToDB(value: T): Any = getter.get(value)
    override fun nonNullValueToString(value: T): String = "'$value'"
    override fun valueFromDB(value: Any): T = builder(value)
}