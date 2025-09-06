package default_code.column_type

import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ArrayColumnType
import org.jetbrains.exposed.v1.core.CustomEnumerationColumnType
import org.jetbrains.exposed.v1.core.Table
import kotlin.enums.enumEntries
import kotlin.reflect.KClass

fun <E> getArrayColumnType(columnType: ColumnType<E & Any>) =
    ArrayColumnType<E, List<E>>(delegate = columnType)

interface PgEnum {
    val pgEnumTypeName: String
    val pgEnumLabel: String
}

inline fun <reified T> getPgEnumByLabel(label: String): T
        where T : Enum<T>,
              T : PgEnum {
    return enumEntries<T>().singleOrNull { e -> e.pgEnumLabel == label }
        ?: error("enum with label '$label' not found in '${T::class.qualifiedName}'")
}

fun <T> getPgEnumByLabel(clazz: KClass<T>, label: String): T
        where T : Enum<T>,
              T : PgEnum {
    return clazz.java.enumConstants.singleOrNull { e -> e.pgEnumLabel == label }
        ?: error("enum with label '$label' not found in '${clazz.qualifiedName}'")
}

fun <T : Enum<T>> Table.customEnumerationArray(
    name: String,
    sql: String?,
    fromDb: (Any) -> T,
    toDb: (T) -> Any
): Column<List<T>> {
    val enumColumnType = CustomEnumerationColumnType(
        name = "${name}_element",
        sql = sql,
        fromDb = fromDb,
        toDb = toDb,
    )
    return array(name = name, columnType = enumColumnType)
}

inline fun <reified T : Any> Table.domainType(
    name: String,
    sqlType: String,
    noinline builder: (Any) -> T,
): Column<T> {
    val type = DomainColumnType(kClass = T::class, sqlType = sqlType, builder = builder)
    return registerColumn(name = name, type = type)
}

fun Table.pgVector(
    name: String,
    schema: String,
): Column<FloatArray> {
    val type = PgVectorColumnType(schema = schema)
    return registerColumn(name = name, type = type)
}

internal fun parseFloatArray(data: String): FloatArray {
    val cleaned = data.trim().removePrefix("[").removeSuffix("]")
    return cleaned.split(",").map { it.trim().toFloat() }.toFloatArray()
}

interface StringLike {
    val value: String
}
