package default_code.column_type

import io.r2dbc.postgresql.codec.Vector

internal fun parseFloatArrayFormDb(value: Any): FloatArray {
    return when (value) {
        is String -> value.let(::parseFloatArray)
        is Vector -> value.vector
        else -> error("Cannot convert $value to FloatArray")
    }
}

fun PgEnum.toDbObject() = this
