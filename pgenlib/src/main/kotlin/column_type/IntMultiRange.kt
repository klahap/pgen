package column_type

import org.jetbrains.exposed.sql.*


public class Int4MultiRangeColumnType : MultiRangeColumnType<Int>() {
    override fun sqlType(): String = "INT4MULTIRANGE"
    override fun String.parse(): MultiRange<Int> = parseMultiRange().toInt4MultiRange()
}

public class Int8MultiRangeColumnType : MultiRangeColumnType<Long>() {
    override fun sqlType(): String = "INT8MULTIRANGE"
    override fun String.parse(): MultiRange<Long> = parseMultiRange().toInt8MultiRange()
}

public fun Table.int4MultiRange(name: String): Column<MultiRange<Int>> =
    registerColumn(name, Int4MultiRangeColumnType())

public fun Table.int8MultiRange(name: String): Column<MultiRange<Long>> =
    registerColumn(name, Int8MultiRangeColumnType())
