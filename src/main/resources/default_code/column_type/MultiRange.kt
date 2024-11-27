package default_code.column_type


@JvmInline
value class MultiRange<T : Comparable<T>>(
    private val ranges: Set<ClosedRange<T>>
) : Set<ClosedRange<T>> by ranges
