package column_type

import org.jetbrains.exposed.sql.ColumnType
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.SQLException


public class UnconstrainedNumericColumnType : ColumnType<BigDecimal>() {
    override fun sqlType(): String = "NUMERIC"

    override fun readObject(rs: ResultSet, index: Int): Any? {
        return rs.getObject(index)
    }

    override fun valueFromDB(value: Any): BigDecimal = when (value) {
        is BigDecimal -> value
        is Double -> {
            if (value.isNaN())
                throw SQLException("Unexpected value of type Double: NaN of ${value::class.qualifiedName}")
            else
                value.toBigDecimal()
        }

        is Float -> {
            if (value.isNaN())
                error("Unexpected value of type Float: NaN of ${value::class.qualifiedName}")
            else
                value.toBigDecimal()
        }

        is Long -> value.toBigDecimal()
        is Int -> value.toBigDecimal()
        is Short -> value.toLong().toBigDecimal()
        else -> error("Unexpected value of type Numeric: $value of ${value::class.qualifiedName}")
    }
}