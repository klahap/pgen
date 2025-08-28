package io.github.klahap.pgen.util

import io.github.klahap.pgen.model.config.Config
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource


fun Config.Db.toFlywayOrNull(): Flyway? {
    val connConfig = connectionConfig ?: return null
    val flywayConfig = flyway ?: return null
    val dataSource = PGSimpleDataSource().apply {
        setUrl(connConfig.url)
        user = connConfig.user
        password = connConfig.password
    }
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("filesystem:${flywayConfig.migrationDirectory}")
        .load() ?: error("no flyway instance")
}