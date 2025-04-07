package default_code.util

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction


fun <T> Database.transaction(block: Transaction.() -> T) =
    org.jetbrains.exposed.sql.transactions.transaction(db = this) { block() }

suspend fun <T> Database.suspendTransaction(
    context: CoroutineContext = Dispatchers.IO,
    readOnly: Boolean = false,
    block: suspend Transaction.() -> T,
) = newSuspendedTransaction(context, db = this, readOnly = readOnly) { block() }
