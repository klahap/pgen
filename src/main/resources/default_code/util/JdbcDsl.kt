package default_code.util

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.ISqlExpressionBuilder
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction as jdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.core.ArrayColumnType
import org.jetbrains.exposed.v1.core.ColumnSet
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select


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
    context(_: Transaction, _: ProducerScope<T>)
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

fun ColumnSet.select(builder: MutableList<Expression<*>>.() -> Unit): Query =
    select(buildList(builder))
