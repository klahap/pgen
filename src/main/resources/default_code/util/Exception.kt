package default_code.util


sealed class PgenException(
    val status: Int,
    message: String?,
    throwable: Throwable?,
) : RuntimeException(message, throwable)

class MultipleEntitiesException(
    message: String? = null,
    throwable: Throwable? = null
) : PgenException(status = 409, message = message, throwable = throwable)

class NoEntityException(
    message: String? = null,
    throwable: Throwable? = null
) : PgenException(status = 404, message = message, throwable = throwable)
