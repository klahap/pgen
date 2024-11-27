package column_type

import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

public abstract class RangeColumnType<T : Comparable<T>, R : ClosedRange<T>> : ColumnType<R>() {
    public abstract fun String.parse(): R

    override fun nonNullValueToString(value: R): String = value.toPgRangeString()

    override fun nonNullValueAsDefaultString(value: R): String =
        "'${nonNullValueToString(value)}'"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val parameterValue: PGobject? = value?.let {
            PGobject().apply {
                type = sqlType()
                this.value = @Suppress("UNCHECKED_CAST") nonNullValueToString(it as R)
            }
        }
        super.setParameter(stmt, index, parameterValue)
    }

    override fun valueFromDB(value: Any): R? = when (value) {
        is PGobject -> value.value?.takeIf { it.isNotBlank() }?.parse()
        else -> error("Retrieved unexpected value of type ${value::class.simpleName}")
    }
}
