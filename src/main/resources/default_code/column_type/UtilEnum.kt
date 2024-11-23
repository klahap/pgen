package default_code.column_type

import org.postgresql.util.PGobject
import kotlin.enums.enumEntries

interface PgEnum {
    val pgEnumTypeName: String
    val pgEnumLabel: String

    fun toPgObject() = PGobject().apply {
        value = pgEnumLabel
        type = pgEnumTypeName
    }
}

inline fun <reified T> getPgEnumByLabel(label: String): T
        where T : Enum<T>,
              T : PgEnum {
    return enumEntries<T>().singleOrNull { e -> e.pgEnumLabel == label }
        ?: error("enum with label '$label' not found in '${T::class.qualifiedName}'")
}
