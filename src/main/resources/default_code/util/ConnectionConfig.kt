package default_code.util

import kotlin.time.Duration

sealed interface ConnectionConfig {
    val subProtocol: String
    val host: String
    val port: Int?
    val database: String
    val query: Map<String, String>

    sealed interface Async : ConnectionConfig

    private val protocol
        get() = when (this) {
            is R2dbcPool -> Protocol.R2DBC_POOL
            is R2dbc -> Protocol.R2DBC
            is Jdbc -> Protocol.JDBC
        }

    private val queryStr
        get() = query.entries.joinToString(
            prefix = if (query.isEmpty()) "" else "?",
            separator = "&",
        ) { "${it.key}=${it.value}" }

    fun url(): String = url(creds = null)
    fun url(user: String, password: String): String = url(creds = "$user:$password")
    private fun url(creds: String?): String =
        "${protocol.value}:$subProtocol://${creds?.let { "$it@" } ?: ""}$host:$port/$database$queryStr"

    fun toR2dbcPool(block: R2dbcPool.Builder.() -> Unit = {}): R2dbcPool = R2dbcPool.Builder(
        subProtocol = subProtocol,
        host = host,
        port = port,
        database = database,
        query = query.toMutableMap(),
    ).apply(block).build()

    fun toR2dbc(block: R2dbc.Builder.() -> Unit = {}): R2dbc = R2dbc.Builder(
        subProtocol = subProtocol,
        host = host,
        port = port,
        database = database,
        query = query.toMutableMap(),
    ).apply(block).build()

    fun toJdbc(block: Jdbc.Builder.() -> Unit = {}): Jdbc = Jdbc.Builder(
        subProtocol = subProtocol,
        host = host,
        port = port,
        database = database,
        query = query.toMutableMap(),
    ).apply(block).build()


    private enum class Protocol(val value: String) {
        JDBC("jdbc"),
        R2DBC("r2dbc"),
        R2DBC_POOL("r2dbc:pool"),
    }

    data class R2dbcPool(
        override val subProtocol: String,
        override val host: String,
        override val port: Int?,
        override val database: String,
        override val query: Map<String, String>,
    ) : Async {
        data class Builder(
            var subProtocol: String,
            var host: String,
            var port: Int?,
            var database: String,
            val query: MutableMap<String, String>,
        ) {
            /**
             * Number of retries if the first connection acquisition attempt fails. Defaults to 1.
             */
            fun acquireRetry(value: Int) {
                query.put("acquireRetry", value.toString())
            }

            /**
             * Interval for background eviction enabling background eviction. Disabled by default. Setting the value to
             * Duration.ZERO disables background eviction even if maxIdleTime is configured.
             */
            fun backgroundEvictionInterval(interval: Duration) {
                query.put("backgroundEvictionInterval", interval.toIsoString())
            }

            /**
             * Initial pool size. Defaults to 10.
             */
            fun initialSize(value: Int) {
                query.put("initialSize", value.toString())
            }

            /**
             *	Minimum idle connection count. Defaults to 0.
             */
            fun minIdle(value: Int) {
                query.put("minIdle", value.toString())
            }

            /**
             *	Maximum pool size. Defaults to 10.
             */
            fun maxSize(value: Int) {
                query.put("maxSize", value.toString())
            }

            /**
             * Maximum lifetime of the connection in the pool. Negative values indicate no timeout. Defaults to no timeout.
             */
            fun maxLifeTime(value: Duration) {
                query.put("maxLifeTime", value.toIsoString())
            }

            /**
             * Maximum idle time of the connection in the pool. Negative values indicate no timeout. Defaults to 30 minutes.
             * This value is used as an interval for background eviction of idle connections unless configuring backgroundEvictionInterval.
             */
            fun maxIdleTime(value: Duration) {
                query.put("maxIdleTime", value.toIsoString())
            }

            /**
             * Maximum time to acquire connection from pool. Negative values indicate no timeout. Defaults to no timeout.
             */
            fun maxAcquireTime(value: Duration) {
                query.put("maxAcquireTime", value.toIsoString())
            }

            /**
             * Maximum time to create a new connection. Negative values indicate no timeout. Defaults to no timeout.
             */
            fun maxCreateConnectionTime(value: Duration) {
                query.put("maxCreateConnectionTime", value.toIsoString())
            }

            /**
             * Maximum time to validate connection from pool. Negative values indicate no timeout. Defaults to no timeout.
             */
            fun maxValidationTime(value: Duration) {
                query.put("maxValidationTime", value.toIsoString())
            }

            fun build() = R2dbcPool(
                subProtocol = subProtocol,
                host = host,
                port = port,
                database = database,
                query = query.toMap(),
            )
        }
    }

    data class R2dbc(
        override val subProtocol: String,
        override val host: String,
        override val port: Int?,
        override val database: String,
        override val query: Map<String, String>,
    ) : Async {
        data class Builder(
            var subProtocol: String,
            var host: String,
            var port: Int?,
            var database: String,
            val query: MutableMap<String, String>,
        ) {
            fun build() = R2dbc(
                subProtocol = subProtocol,
                host = host,
                port = port,
                database = database,
                query = query.toMap(),
            )
        }
    }

    data class Jdbc(
        override val subProtocol: String,
        override val host: String,
        override val port: Int?,
        override val database: String,
        override val query: Map<String, String>,
    ) : ConnectionConfig {
        data class Builder(
            var subProtocol: String,
            var host: String,
            var port: Int?,
            var database: String,
            val query: MutableMap<String, String>,
        ) {
            fun build() = Jdbc(
                subProtocol = subProtocol,
                host = host,
                port = port,
                database = database,
                query = query.toMap(),
            )
        }
    }

    companion object {
        fun parse(url: String): ConnectionConfig {
            val regex = Regex("""([\w:]+):(\w+)://([^:/]+)(?::(\d+))?/([\w-]+)(?:\?(.+))?""")
            val match = regex.matchEntire(url)
                ?: throw IllegalArgumentException("Unsupported or invalid DB URL")
            val (protocolStr, subProtocol, host, portStr, database, queryStr) = match.destructured
            val protocol = Protocol.entries.firstOrNull { it.value.equals(protocolStr, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported protocol '$protocolStr'")
            val query = queryStr.split("&").filter { '=' in it }
                .associate { it.substringBefore('=') to it.substringAfter('=') }
            val connectionConfig = when (protocol) {
                Protocol.R2DBC_POOL -> R2dbcPool(
                    subProtocol = subProtocol,
                    host = host,
                    port = portStr.toIntOrNull(),
                    database = database,
                    query = query,
                )

                Protocol.JDBC -> Jdbc(
                    subProtocol = subProtocol,
                    host = host,
                    port = portStr.toIntOrNull(),
                    database = database,
                    query = query,
                )

                Protocol.R2DBC -> R2dbc(
                    subProtocol = subProtocol,
                    host = host,
                    port = portStr.toIntOrNull(),
                    database = database,
                    query = query,
                )
            }
            return connectionConfig
        }
    }
}
