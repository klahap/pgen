package default_code.util

interface IConnectionProperties {
    val url: String
    val username: String
    val password: String

    fun toConnectionConfig(): ConnectionConfig = ConnectionConfig.parse(url)
}
