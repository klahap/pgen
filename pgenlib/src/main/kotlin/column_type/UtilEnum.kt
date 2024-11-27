package column_type

import org.postgresql.util.PGobject
import kotlin.enums.enumEntries

public interface PgEnum {
    public val pgEnumTypeName: String
    public val pgEnumLabel: String

    public fun toPgObject(): PGobject = PGobject().apply {
        value = pgEnumLabel
        type = pgEnumTypeName
    }
}

public inline fun <reified T> getPgEnumByLabel(label: String): T
        where T : Enum<T>,
              T : PgEnum {
    return enumEntries<T>().singleOrNull { e -> e.pgEnumLabel == label }
        ?: error("enum with label '$label' not found in '${T::class.qualifiedName}'")
}
