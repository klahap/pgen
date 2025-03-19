package default_code.column_type

import org.jetbrains.exposed.sql.ColumnType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class DomainColumnType<T : Any>(
    kClass: KClass<T>,
    private val sqlType: String,
) : ColumnType<T>() {
    private val builder: (Any) -> T
    private val getter: KProperty1<T, Any>

    init {
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("${kClass.qualifiedName} must have a primary constructor")
        if (constructor.parameters.size != 1)
            throw IllegalArgumentException("${kClass.qualifiedName} must have a primary constructor with a single parameter")
        @Suppress("UNCHECKED_CAST")
        getter = (kClass.memberProperties.singleOrNull() as? KProperty1<T, Any>)
            ?: throw IllegalArgumentException("${kClass.qualifiedName} must have a single non nullable property")
        builder = { constructor.call(it) }
    }

    override fun sqlType(): String = sqlType
    override fun notNullValueToDB(value: T): Any = getter.get(value)
    override fun valueFromDB(value: Any): T = builder(value)
}