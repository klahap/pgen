package default_code.util

sealed interface UpdateField<out T : Any> {
    sealed interface Nullable<out T : Any> : UpdateField<T>
    sealed interface NotNullable<out T : Any> : UpdateField<T>

    /** do not touch */
    object Unset : Nullable<Nothing>, NotNullable<Nothing>

    /** set to null */
    object Null : Nullable<Nothing>

    /** set to value */
    data class Value<T: Any>(val value: T) : Nullable<T>, NotNullable<T>

    companion object {
        fun <T: Any> T.asUpdate() = UpdateField.Value(this)
    }
}