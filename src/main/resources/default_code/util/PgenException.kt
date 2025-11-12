package default_code.util

import io.github.goquati.kotlin.util.Result
import io.github.goquati.kotlin.util.failureOrNull

data class PgenErrorDetails(
    val code: String,
    val columnName: String?,
    val constraintName: String?,
    val dataTypeName: String?,
    val detail: String?,
    val file: String?,
    val hint: String?,
    val internalPosition: String?,
    val internalQuery: String?,
    val line: String?,
    val message: String,
    val position: String?,
    val routine: String?,
    val schemaName: String?,
    val severityLocalized: String,
    val severityNonLocalized: String,
    val tableName: String?,
    val where: String?,
)

sealed class PgenException(
    val msg: String,
    throwable: Throwable? = null,
) : RuntimeException(msg, throwable) {
    companion object Companion {
        internal fun of(details: PgenErrorDetails, t: Throwable? = null): PgenException {
            val code = details.code.uppercase()
            val msg = details.message
            return when (code) {
                "23000" -> IntegrityConstraintViolation(details = details, msg = msg, t = t)
                "23001" -> RestrictViolation(details = details, msg = msg, t = t)
                "23502" -> NotNullViolation(details = details, msg = msg, t = t)
                "23503" -> ForeignKeyViolation(details = details, msg = msg, t = t)
                "23505" -> UniqueViolation(details = details, msg = msg, t = t)
                "23514" -> CheckViolation(details = details, msg = msg, t = t)
                "23P01" -> ExclusionViolation(details = details, msg = msg, t = t)
                else -> Generic(details = details, msg = msg, t = t)
            }
        }
    }

    sealed class Sql(
        val details: PgenErrorDetails,
        msg: String,
        throwable: Throwable? = null,
    ) : PgenException(msg, throwable)

    class IntegrityConstraintViolation(details: PgenErrorDetails, msg: String, t: Throwable? = null) :
        Sql(details, msg, t)

    class RestrictViolation(details: PgenErrorDetails, msg: String, t: Throwable? = null) : Sql(details, msg, t)
    class NotNullViolation(details: PgenErrorDetails, msg: String, t: Throwable? = null) : Sql(details, msg, t)
    class ForeignKeyViolation(details: PgenErrorDetails, msg: String, t: Throwable? = null) : Sql(details, msg, t)
    class UniqueViolation(details: PgenErrorDetails, msg: String, t: Throwable? = null) : Sql(details, msg, t)
    class CheckViolation(details: PgenErrorDetails, msg: String, t: Throwable? = null) : Sql(details, msg, t)
    class ExclusionViolation(details: PgenErrorDetails, msg: String, t: Throwable? = null) : Sql(details, msg, t)
    class Generic(details: PgenErrorDetails, msg: String, t: Throwable? = null) : Sql(details, msg, t)
    class Other(msg: String, t: Throwable? = null) : PgenException(msg, t)
}

inline fun <T> Result<T, PgenException>.onIntegrityConstraintViolation(block: (PgenException.IntegrityConstraintViolation) -> Unit) =
    apply { (failureOrNull as? PgenException.IntegrityConstraintViolation)?.also { block(it) } }

inline fun <T> Result<T, PgenException>.onRestrictViolation(block: (PgenException.RestrictViolation) -> Unit) =
    apply { (failureOrNull as? PgenException.RestrictViolation)?.also { block(it) } }

inline fun <T> Result<T, PgenException>.onNotNullViolation(block: (PgenException.NotNullViolation) -> Unit) =
    apply { (failureOrNull as? PgenException.NotNullViolation)?.also { block(it) } }

inline fun <T> Result<T, PgenException>.onForeignKeyViolation(block: (PgenException.ForeignKeyViolation) -> Unit) =
    apply { (failureOrNull as? PgenException.ForeignKeyViolation)?.also { block(it) } }

inline fun <T> Result<T, PgenException>.onUniqueViolation(block: (PgenException.UniqueViolation) -> Unit) =
    apply { (failureOrNull as? PgenException.UniqueViolation)?.also { block(it) } }

inline fun <T> Result<T, PgenException>.onCheckViolation(block: (PgenException.CheckViolation) -> Unit) =
    apply { (failureOrNull as? PgenException.CheckViolation)?.also { block(it) } }

inline fun <T> Result<T, PgenException>.onExclusionViolation(block: (PgenException.ExclusionViolation) -> Unit) =
    apply { (failureOrNull as? PgenException.ExclusionViolation)?.also { block(it) } }

inline fun <T> Result<T, PgenException>.onSqlException(block: (PgenException.Sql) -> Unit) =
    apply { (failureOrNull as? PgenException.Sql)?.also { block(it) } }

inline fun <T> Result<T, PgenException>.onNoneSqlException(block: (PgenException.Other) -> Unit) =
    apply { (failureOrNull as? PgenException.Other)?.also { block(it) } }