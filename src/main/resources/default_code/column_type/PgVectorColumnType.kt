package default_code.column_type

import org.jetbrains.exposed.v1.core.ColumnType


class PgVectorColumnType(
    private val schema: String,
) : ColumnType<FloatArray>() {
    override fun sqlType(): String = "${schema}.vector"
    override fun notNullValueToDB(value: FloatArray) = value
    override fun valueFromDB(value: Any): FloatArray = parseFloatArrayFormDb(value)
}