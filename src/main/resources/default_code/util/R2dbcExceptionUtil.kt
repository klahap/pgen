package default_code.util

import io.github.goquati.kotlin.util.Result
import io.github.goquati.kotlin.util.mapError
import io.r2dbc.postgresql.api.ErrorDetails
import io.r2dbc.postgresql.api.PostgresqlException
import org.jetbrains.exposed.v1.r2dbc.ExposedR2dbcException
import kotlin.jvm.optionals.getOrNull

fun ErrorDetails.toPgenErrorDetails() = PgenErrorDetails(
    code = code,
    columnName = columnName.getOrNull(),
    constraintName = constraintName.getOrNull(),
    dataTypeName = dataTypeName.getOrNull(),
    detail = detail.getOrNull(),
    file = file.getOrNull(),
    hint = hint.getOrNull(),
    internalPosition = internalPosition.getOrNull(),
    internalQuery = internalQuery.getOrNull(),
    line = line.getOrNull(),
    message = message,
    position = position.getOrNull(),
    routine = routine.getOrNull(),
    schemaName = schemaName.getOrNull(),
    severityLocalized = severityLocalized,
    severityNonLocalized = severityNonLocalized,
    tableName = tableName.getOrNull(),
    where = where.getOrNull(),
)

internal fun <T> kotlin.Result<T>.mapPgenError(): Result<T, PgenException> = mapError { t ->
    when (t) {
        is ExposedR2dbcException -> when (val e = t.cause) {
            is PostgresqlException -> PgenException.of(
                details = e.errorDetails.toPgenErrorDetails(),
                t = t,
            )

            else -> PgenException.Other(msg = t.message ?: "", t = t)
        }

        else -> PgenException.Other(msg = t.message ?: "", t = t)
    }
}
