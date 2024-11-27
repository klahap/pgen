package default_code.column_type

internal fun List<RawRange>.toInt4MultiRange(): MultiRange<Int> = MultiRange(map { it.toInt4Range() }.toSet())
internal fun List<RawRange>.toInt8MultiRange(): MultiRange<Long> = MultiRange(map { it.toInt8Range() }.toSet())

internal fun String.parseMultiRange(): List<RawRange> = trimStart('{').trimEnd('}')
    .split(',').chunked(2)
    .map { borders -> borders.joinToString(",") }
    .map { it.parseRange() }
