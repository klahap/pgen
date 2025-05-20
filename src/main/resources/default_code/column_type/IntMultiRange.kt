package default_code.column_type

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table


class Int4MultiRangeColumnType : MultiRangeColumnType<Int>() {
    override fun sqlType(): String = "INT4MULTIRANGE"
    override fun String.parse(): MultiRange<Int> = parseMultiRange().toInt4MultiRange()
}

class Int8MultiRangeColumnType : MultiRangeColumnType<Long>() {
    override fun sqlType(): String = "INT8MULTIRANGE"
    override fun String.parse(): MultiRange<Long> = parseMultiRange().toInt8MultiRange()
}

fun Table.int4MultiRange(name: String): Column<MultiRange<Int>> = registerColumn(name, Int4MultiRangeColumnType())
fun Table.int8MultiRange(name: String): Column<MultiRange<Long>> = registerColumn(name, Int8MultiRangeColumnType())
