package io.github.klahap.pgen.dsl

import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.ResultSet


fun <T> Connection.executeQuery(@Language("sql") query: String, mapper: (ResultSet) -> T): List<T> {
    return createStatement().use { statement ->
        statement.executeQuery(query).use { rs ->
            buildList { while (rs.next()) add(mapper(rs)) }
        }
    }
}
