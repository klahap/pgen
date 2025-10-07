package default_code.column_type

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import shared_code.RegClass

object RegClassColumnType : ColumnType<RegClass>() {
    override fun sqlType(): String = "regclass"
    override fun notNullValueToDB(value: RegClass): Any = value.name
    override fun valueFromDB(value: Any) = RegClass.of(value as String)
}

fun Table.regClass(name: String): Column<RegClass> {
    return registerColumn(name = name, type = RegClassColumnType)
}