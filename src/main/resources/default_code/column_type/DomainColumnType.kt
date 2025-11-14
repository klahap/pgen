package default_code.column_type

import org.jetbrains.exposed.v1.core.ColumnType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class DomainColumnType<T : Any>(
    kClass: KClass<T>,
    private val sqlType: String,
    private val builder: (Any) -> T,
) : ColumnType<T>() {
    private val getter: KProperty1<T, Any>

    init {
        @Suppress("UNCHECKED_CAST")
        getter = (kClass.memberProperties.singleOrNull() as? KProperty1<T, Any>)
            ?: throw IllegalArgumentException("${kClass.qualifiedName} must have a single non nullable property")
    }

    override fun sqlType(): String = sqlType
    override fun notNullValueToDB(value: T): Any = getter.get(value)
    override fun nonNullValueToString(value: T): String = "'$value'"
    override fun valueFromDB(value: Any): T = builder(value)
}