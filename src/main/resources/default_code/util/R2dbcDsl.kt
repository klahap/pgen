package default_code.util

import io.r2dbc.spi.IsolationLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ColumnSet
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction as r2dbcSuspendTransaction
import kotlin.coroutines.CoroutineContext
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.postgresql.extension.CodecRegistrar
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager
import default_code.column_type.SqlStringHelper
import shared_code.ILocalConfigContext

fun ColumnSet.select(builder: MutableList<Expression<*>>.() -> Unit): Query =
    select(buildList(builder))

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

fun IConnectionProperties.toR2dbcDatabase(
    codecs: List<CodecRegistrar> = emptyList(),
    connectionConfig: PostgresqlConnectionConfiguration.Builder.(ConnectionConfig) -> Unit = {},
    databaseConfig: R2dbcDatabaseConfig.Builder.(ConnectionConfig) -> Unit = {},
): R2dbcDatabase {
    val config = toConnectionConfig().toJdbc()
    val options = PostgresqlConnectionConfiguration.builder().apply {
        host(config.host)
        config.port?.let(::port)
        database(config.database)
        username(username)
        password(password)
        codecs.forEach(::codecRegistrar)
        connectionConfig(config)
    }.build()
    val cxFactory = PostgresqlConnectionFactory(options)
    val db = R2dbcDatabase.connect(
        connectionFactory = cxFactory,
        databaseConfig = R2dbcDatabaseConfig {
            explicitDialect = PostgreSQLDialect()
            databaseConfig(config)
        }
    )
    return db
}

suspend fun <T> R2dbcDatabase.suspendTransaction(
    transactionIsolation: IsolationLevel? = null,
    readOnly: Boolean = false,
    statement: suspend R2dbcTransaction.() -> T
): T {
    val isolation = transactionIsolation ?: transactionManager.defaultIsolationLevel
    require(isolation != null) { "A default isolation level for this transaction has not been set" }
    val result = r2dbcSuspendTransaction(
        transactionIsolation = isolation,
        readOnly = readOnly,
        db = this,
        statement = statement,
    )
    return result
}

fun <T> R2dbcDatabase.blockingTransaction(
    context: CoroutineContext = Dispatchers.IO,
    readOnly: Boolean = false,
    block: suspend R2dbcTransaction.() -> T,
) = runBlocking(context) {
    val result = suspendTransaction(readOnly = readOnly, statement = block)
    result
}

fun interface TransactionFlowScope<T> {
    context(_: Transaction, _: ProducerScope<T>)
    suspend fun block()
}

fun <T> R2dbcDatabase.transactionFlow(
    readOnly: Boolean = false,
    block: TransactionFlowScope<T>,
): Flow<T> {
    return channelFlow {
        this@transactionFlow.suspendTransaction(
            readOnly = readOnly,
        ) {
            block.block()
        }
    }
}

suspend fun R2dbcTransaction.setLocalConfig(key: String, value: String) {
    val sql = SqlStringHelper.buildSetLocalConfigSql(key = key, value = value)
    exec(sql)
}

suspend fun R2dbcTransaction.setLocalConfig(config: ILocalConfigContext) = setLocalConfig(config.data)
suspend fun R2dbcTransaction.setLocalConfig(config: Map<String, String>) {
    if (config.isEmpty()) return
    val sql = SqlStringHelper.buildSetLocalConfigSql(config)
    exec(sql)
}
context(c: ILocalConfigContext)
suspend fun <T> R2dbcDatabase.suspendTransactionWithContext(
    transactionIsolation: IsolationLevel? = null,
    readOnly: Boolean = false,
    statement: suspend R2dbcTransaction.() -> T
): T = this.suspendTransaction(
    transactionIsolation = transactionIsolation,
    readOnly = readOnly,
) {
    setLocalConfig(c)
    statement()
}

context(c: ILocalConfigContext)
fun <T> R2dbcDatabase.blockingTransactionWithContext(
    context: CoroutineContext = Dispatchers.IO,
    readOnly: Boolean = false,
    block: suspend R2dbcTransaction.() -> T,
) = this.blockingTransaction(
    context = context,
    readOnly = readOnly,
) {
    setLocalConfig(c)
    block()
}

context(c: ILocalConfigContext)
fun <T> R2dbcDatabase.transactionFlowWithContext(
    readOnly: Boolean = false,
    block: TransactionFlowScope<T>,
): Flow<T> {
    return channelFlow {
        this@transactionFlowWithContext.suspendTransaction(
            readOnly = readOnly,
        ) {
            setLocalConfig(c)
            block.block()
        }
    }
}
