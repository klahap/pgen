package default_code.util

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction as jdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

fun Database.Companion.connect(
    connectionConfig: ConnectionConfig.Jdbc,
    user: String,
    password: String,
): Database {
    val url = connectionConfig.url()
    val db = Database.connect(
        url = url,
        user = user,
        password = password
    )
    return db
}

fun <T> Database.transaction(block: Transaction.() -> T) =
    jdbcTransaction(db = this) { block() }

suspend fun <T> Database.suspendTransaction(
    context: CoroutineContext = Dispatchers.IO,
    readOnly: Boolean = false,
    block: suspend Transaction.() -> T,
) = newSuspendedTransaction(context, db = this, readOnly = readOnly) { block() }

fun interface TransactionFlowScope<T> {
    context(Transaction, ProducerScope<T>)
    suspend fun block()
}

fun <T> Database.transactionFlow(
    context: CoroutineContext = Dispatchers.IO,
    readOnly: Boolean = false,
    block: TransactionFlowScope<T>,
): Flow<T> {
    return channelFlow {
        this@transactionFlow.suspendTransaction(
            context = context,
            readOnly = readOnly,
        ) {
            block.block()
        }
    }
}
