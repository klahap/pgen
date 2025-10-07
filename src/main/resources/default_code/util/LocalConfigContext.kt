package default_code.util

interface ILocalConfigContext {
    val data: Map<String, String>
}

@JvmInline
value class LocalConfigContext(override val data: Map<String, String>) : ILocalConfigContext
