package default_code.util

import io.r2dbc.spi.IsolationLevel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction as r2dbcSuspendTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransactionAsync as r2dbcSuspendTransactionAsync
import kotlin.coroutines.CoroutineContext


fun R2dbcDatabase.Companion.connect(
    connectionConfig: ConnectionConfig.Async,
    user: String,
    password: String,
): R2dbcDatabase {
    val url = connectionConfig.url(user = user, password = password)
    val db = R2dbcDatabase.connect(url = url)
    return db
}

suspend fun <T> R2dbcDatabase.suspendTransaction(
    context: CoroutineContext = Dispatchers.IO,
    transactionIsolation: IsolationLevel? = null,
    readOnly: Boolean = false,
    statement: suspend R2dbcTransaction.() -> T
): T {
    val result = r2dbcSuspendTransaction(
        context = context,
        transactionIsolation = transactionIsolation,
        readOnly = readOnly,
        db = this,
        statement = statement,
    )
    return result
}

suspend fun <T> R2dbcDatabase.suspendTransactionAsync(
    context: CoroutineContext = Dispatchers.IO,
    transactionIsolation: IsolationLevel? = null,
    readOnly: Boolean = false,
    statement: suspend R2dbcTransaction.() -> T
): Deferred<T> {
    return r2dbcSuspendTransactionAsync(
        context = context,
        db = this,
        transactionIsolation = transactionIsolation,
        readOnly = readOnly,
        statement = statement,
    )
}

fun <T> R2dbcDatabase.blockingTransaction(
    context: CoroutineContext = Dispatchers.IO,
    readOnly: Boolean = false,
    block: suspend R2dbcTransaction.() -> T,
) = runBlocking(context) {
    val result = suspendTransaction(context = context, readOnly = readOnly, statement = block)
    result
}

fun interface TransactionFlowScope<T> {
    context(Transaction, ProducerScope<T>)
    suspend fun block()
}

fun <T> R2dbcDatabase.transactionFlow(
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