package column_type


@JvmInline
public value class MultiRange<T : Comparable<T>>(
    private val ranges: Set<ClosedRange<T>>
) : Set<ClosedRange<T>> by ranges
