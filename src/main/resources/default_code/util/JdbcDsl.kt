package default_code.util

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction as jdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

fun <T> Database.transaction(block: Transaction.() -> T) =
    jdbcTransaction(db = this) { block() }

suspend fun <T> Database.suspendTransaction(
    context: CoroutineContext = Dispatchers.IO,
    readOnly: Boolean = false,
    block: suspend Transaction.() -> T,
) = newSuspendedTransaction(context, db = this, readOnly = readOnly) { block() }
