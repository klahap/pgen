package default_code.util

import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.compoundAnd
import org.jetbrains.exposed.v1.core.compoundOr


operator fun <T> ResultRow.get(column: Column<T>, alias: Alias<*>?): T = when (alias) {
    null -> this[column]
    else -> this[alias[column]]
}

fun compoundAnd(con: Op<Boolean>?, vararg cons: Op<Boolean>?): Op<Boolean> =
    (listOf(con) + cons).filterNotNull().compoundAnd()

fun compoundOr(con: Op<Boolean>?, vararg cons: Op<Boolean>?): Op<Boolean> =
    (listOf(con) + cons).filterNotNull().compoundOr()
