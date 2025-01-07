package default_code.column_type

import default_code.column_type.RawRange.Empty
import default_code.column_type.RawRange.Normal
import default_code.column_type.RawRangeBorder.Infinity
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.ArrayColumnType
import org.postgresql.util.PGobject
import kotlin.enums.enumEntries

fun <E> getArrayColumnType(columnType: ColumnType<E & Any>) =
    ArrayColumnType<E, List<E>>(delegate = columnType)

interface PgEnum {
    val pgEnumTypeName: String
    val pgEnumLabel: String

    fun toPgObject() = PGobject().apply {
        value = pgEnumLabel
        type = pgEnumTypeName
    }
}

inline fun <reified T> getPgEnumByLabel(label: String): T
        where T : Enum<T>,
              T : PgEnum {
    return enumEntries<T>().singleOrNull { e -> e.pgEnumLabel == label }
        ?: error("enum with label '$label' not found in '${T::class.qualifiedName}'")
}

internal fun List<RawRange>.toInt4MultiRange(): MultiRange<Int> = MultiRange(map { it.toInt4Range() }.toSet())
internal fun List<RawRange>.toInt8MultiRange(): MultiRange<Long> = MultiRange(map { it.toInt8Range() }.toSet())
internal fun String.parseMultiRange(): List<RawRange> = trimStart('{').trimEnd('}')
    .split(',').chunked(2)
    .map { borders -> borders.joinToString(",") }
    .map { it.parseRange() }


internal sealed interface RawRange {
    data object Empty : RawRange
    data class Normal(val start: RawRangeBorder, val end: RawRangeBorder) : RawRange
}

internal sealed interface RawRangeBorder {
    data object Infinity : RawRangeBorder
    data class Normal(
        val value: String,
        val inclusive: Boolean,
    ) : RawRangeBorder
}

internal fun RawRange.toInt4Range(): IntRange = when (this) {
    Empty -> IntRange.EMPTY
    is Normal -> IntRange(
        start = when (start) {
            RawRangeBorder.Infinity -> Int.MIN_VALUE
            is RawRangeBorder.Normal -> start.value.toInt().let { if (start.inclusive) it else it + 1 }
        },
        endInclusive = when (end) {
            RawRangeBorder.Infinity -> Int.MAX_VALUE
            is RawRangeBorder.Normal -> end.value.toInt().let { if (end.inclusive) it else it - 1 }
        },
    )
}

internal fun RawRange.toInt8Range(): LongRange = when (this) {
    Empty -> LongRange.EMPTY
    is Normal -> LongRange(
        start = when (start) {
            RawRangeBorder.Infinity -> Long.MIN_VALUE
            is RawRangeBorder.Normal -> start.value.toLong().let { if (start.inclusive) it else it + 1 }
        },
        endInclusive = when (end) {
            RawRangeBorder.Infinity -> Long.MAX_VALUE
            is RawRangeBorder.Normal -> end.value.toLong().let { if (end.inclusive) it else it - 1 }
        },
    )
}

internal fun String.parseRangeBorderStart(): RawRangeBorder {
    if (isBlank()) error("invalid range start ''")
    if (this == "(") return Infinity
    return RawRangeBorder.Normal(
        value = trimStart('[', '(').takeIf { it.isNotBlank() } ?: error("invalid range start '$this'"),
        inclusive = when (first()) {
            '[' -> true
            '(' -> false
            else -> error("Retrieved unexpected range start '$this'")
        }
    )
}

internal fun String.parseRangeBorderEnd(): RawRangeBorder {
    if (isBlank()) error("invalid range end ''")
    if (this == ")") return Infinity
    return RawRangeBorder.Normal(
        value = trimStart(']', ')').takeIf { it.isNotBlank() } ?: error("invalid range end '$this'"),
        inclusive = when (last()) {
            ']' -> true
            ')' -> false
            else -> error("Retrieved unexpected range end '$this'")
        }
    )
}

internal fun String.parseRange(): RawRange {
    if (this == "()") return Empty
    val (startRaw, endRaw) = split(",").takeIf { it.size == 2 }
        ?: error("invalid range string '$this'")
    return Normal(
        start = startRaw.parseRangeBorderStart(),
        end = endRaw.parseRangeBorderEnd(),
    )
}

internal fun ClosedRange<*>.toPgRangeString(): String = "[$start,$endInclusive]"
