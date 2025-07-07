package default_code.column_type

import org.postgresql.util.PGobject

internal fun parseFloatArrayFormDb(value: Any): FloatArray {
    return when (value) {
        is PGobject -> value.value?.let(::parseFloatArray) ?: error("Cannot convert null to FloatArray")
        is String -> value.let(::parseFloatArray)
        else -> error("Cannot convert $value to FloatArray")
    }
}

fun PgEnum.toDbObject() = PGobject().apply {
    value = pgEnumLabel
    type = pgEnumTypeName
}
