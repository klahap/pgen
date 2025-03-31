package default_code.column_type

import kotlin.reflect.KClass
import java.util.UUID

@JvmInline
value class PgStructField(val data: String?) {
    companion object {
        private val SPLIT_REGEX = Regex(""",(?![^"]*"(?:(?:[^"]*"){2})*[^"]*$)""")
        fun parseFields(data: String): List<io.github.klahap.pgen_test.db.column_type.PgStructField> {
            return data.trim('(', ')').split(SPLIT_REGEX).map(::PgStructField)
        }
    }
}

fun List<PgStructField>.join() = joinToString(
    separator = ",",
    prefix = "(",
    postfix = ")",
) { it.data ?: "" }

interface PgStructFieldConverter<T> {
    fun serialize(obj: T?): PgStructField
    fun deserialize(obj: PgStructField): T?

    object String : PgStructFieldConverter<kotlin.String> {
        override fun serialize(obj: kotlin.String?) = PgStructField(obj?.escapeString())
        override fun deserialize(obj: PgStructField): kotlin.String? = obj.data?.unescapeString()
    }

    object Short : PgStructFieldConverter<kotlin.Short> {
        override fun serialize(obj: kotlin.Short?) = PgStructField(obj?.toString())
        override fun deserialize(obj: PgStructField): kotlin.Short? = obj.data?.toShortOrNull()
    }

    object Int : PgStructFieldConverter<kotlin.Int> {
        override fun serialize(obj: kotlin.Int?) = PgStructField(obj?.toString())
        override fun deserialize(obj: PgStructField): kotlin.Int? = obj.data?.toIntOrNull()
    }

    object Long : PgStructFieldConverter<kotlin.Long> {
        override fun serialize(obj: kotlin.Long?) = PgStructField(obj?.toString())
        override fun deserialize(obj: PgStructField): kotlin.Long? = obj.data?.toLongOrNull()
    }

    object Float : PgStructFieldConverter<kotlin.Float> {
        override fun serialize(obj: kotlin.Float?) = PgStructField(obj?.toString())
        override fun deserialize(obj: PgStructField): kotlin.Float? = obj.data?.toFloatOrNull()
    }

    object Double : PgStructFieldConverter<kotlin.Double> {
        override fun serialize(obj: kotlin.Double?) = PgStructField(obj?.toString())
        override fun deserialize(obj: PgStructField): kotlin.Double? = obj.data?.toDoubleOrNull()
    }

    @OptIn(ExperimentalStdlibApi::class)
    object ByteArray : io.github.klahap.pgen_test.db.column_type.PgStructFieldConverter<kotlin.ByteArray> {
        override fun serialize(obj: kotlin.ByteArray?) = obj?.toHexString()
            ?.let { "\\x$it".escapeString() }.let(::PgStructField)

        override fun deserialize(obj: io.github.klahap.pgen_test.db.column_type.PgStructField): kotlin.ByteArray? =
            obj.data?.unescapeString()?.removePrefix("\\x")?.hexToByteArray()
    }

    object Uuid : PgStructFieldConverter<UUID> {
        override fun serialize(obj: UUID?) = PgStructField(obj?.toString()?.escapeString())
        override fun deserialize(obj: PgStructField): UUID? = obj.data?.let { UUID.fromString(it) }
    }

    object BigDecimal : PgStructFieldConverter<java.math.BigDecimal> {
        override fun serialize(obj: java.math.BigDecimal?) = PgStructField(obj?.toString())
        override fun deserialize(obj: PgStructField): java.math.BigDecimal? = obj.data?.toBigDecimalOrNull()
    }

    class Enum<E>(
        private val clazz: KClass<E>
    ) : PgStructFieldConverter<E> where E : PgEnum, E : kotlin.Enum<E> {
        override fun serialize(obj: E?) = PgStructField(obj?.pgEnumLabel?.escapeString())
        override fun deserialize(obj: PgStructField): E? = obj.data?.unescapeString()
            ?.let { getPgEnumByLabel(clazz, it) }
    }

    companion object {
        private fun kotlin.String.escapeString() = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
        private fun kotlin.String.unescapeString() = replace("\\\\", "\\").replace("\\\"", "\"").let {
            if (it.startsWith('"') && it.endsWith('"')) it.substring(1, it.length - 1)
            else it
        }
    }
}