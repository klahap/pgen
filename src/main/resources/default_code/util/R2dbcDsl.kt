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
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig


fun R2dbcDatabase.Companion.connect(
    config: ConnectionConfig.Async,
    username: String? = null,
    password: String? = null,
    block: PostgresqlConnectionConfiguration.Builder.() -> Unit = {},
): R2dbcDatabase {
    val options = PostgresqlConnectionConfiguration.builder().apply {
        host(config.host)
        config.port?.let { port(it) }
        database(config.database)
        username?.let { username(it) }
        password?.let { password(it) }
        block()
    }.build()
    val cxFactory = PostgresqlConnectionFactory(options)
    val db = R2dbcDatabase.connect(
        connectionFactory = cxFactory,
        databaseConfig = R2dbcDatabaseConfig {
            explicitDialect = PostgreSQLDialect()
        }
    )
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
    context(_: Transaction, _: ProducerScope<T>)
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