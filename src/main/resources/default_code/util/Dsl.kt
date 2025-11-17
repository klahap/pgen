package default_code.util

import io.github.goquati.kotlin.util.QuatiException
import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.compoundAnd
import org.jetbrains.exposed.v1.core.compoundOr
import org.jetbrains.exposed.v1.core.ArrayColumnType
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.ComparisonOp
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.QueryParameter
import org.jetbrains.exposed.v1.core.anyFrom
import org.jetbrains.exposed.v1.core.QueryBuilder
import shared_code.StringLike

object IsInsert : Expression<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("xmax = 0")
    }
}

object IsUpdate : Expression<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("xmax != 0")
    }
}

operator fun <T> ResultRow.get(column: Column<T>, alias: Alias<*>?): T = when (alias) {
    null -> this[column]
    else -> this[alias[column]]
}

fun compoundAnd(con: Op<Boolean>?, vararg cons: Op<Boolean>?): Op<Boolean> =
    (listOf(con) + cons).filterNotNull().let {
        when (it.size) {
            0 -> Op.TRUE
            1 -> it.single()
            else -> it.compoundAnd()
        }
    }

fun compoundOr(con: Op<Boolean>?, vararg cons: Op<Boolean>?): Op<Boolean> =
    (listOf(con) + cons).filterNotNull().let {
        when (it.size) {
            0 -> Op.TRUE
            1 -> it.single()
            else -> it.compoundOr()
        }
    }

fun <T : Any> arrayAgg(
    elementColumnType: ColumnType<T>,
    exp: Expression<out T?>,
): CustomFunction<List<T?>> = CustomFunction<List<T?>>(
    functionName = "array_agg",
    columnType = ArrayColumnType(elementColumnType),
    exp,
)

fun arrayAgg(exp: Expression<out String?>): CustomFunction<List<String?>> =
    arrayAgg(TextColumnType(), exp)

private class LikeOp(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "LIKE")
private class InsensitiveLikeOp(expr1: Expression<*>, expr2: Expression<*>) : ComparisonOp(expr1, expr2, "ILIKE")
private class ContainsOp(expr1: Expression<List<String>?>, expr2: Expression<String>) :
    ComparisonOp(expr2, anyFrom(expr1), "=")

infix fun <T : StringLike?> ExpressionWithColumnType<T>.like(pattern: String): Op<Boolean> =
    LikeOp(this, QueryParameter(pattern, TextColumnType()))

@JvmName("iLikeString")
infix fun <T : String?> ExpressionWithColumnType<T>.iLike(pattern: T): Op<Boolean> =
    InsensitiveLikeOp(this, QueryParameter(pattern, columnType))

@JvmName("iLikeStringLike")
infix fun <T : StringLike?> ExpressionWithColumnType<T>.iLike(pattern: String): Op<Boolean> =
    InsensitiveLikeOp(this, QueryParameter(pattern, TextColumnType()))

infix fun ExpressionWithColumnType<List<String>?>.arrayContains(pattern: String): Op<Boolean> = ContainsOp(
    expr1 = this,
    expr2 = QueryParameter(value = pattern, sqlType = TextColumnType()),
)

sealed interface DeleteResult {
    fun getOrThrow(msg: String): Unit = when (this) {
        Deleted -> Unit
        None -> throw QuatiException.NotFound("$msg — nothing to delete")
        TooMany -> throw QuatiException.Conflict("$msg — multiple matches found")
    }

    data object None : DeleteResult
    data object Deleted : DeleteResult
    data object TooMany : DeleteResult
}
