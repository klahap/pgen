package column_type

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table


public class Int4RangeColumnType : RangeColumnType<Int, IntRange>() {
    override fun sqlType(): String = "INT4RANGE"
    override fun String.parse(): IntRange = parseRange().toInt4Range()
}

public class Int8RangeColumnType : RangeColumnType<Long, LongRange>() {
    override fun sqlType(): String = "INT8RANGE"
    override fun String.parse(): LongRange = parseRange().toInt8Range()
}

public fun Table.int4Range(name: String): Column<IntRange> = registerColumn(name, Int4RangeColumnType())
public fun Table.int8Range(name: String): Column<LongRange> = registerColumn(name, Int8RangeColumnType())
