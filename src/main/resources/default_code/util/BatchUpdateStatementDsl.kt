package default_code.util

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.BatchDataInconsistentException
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi


open class BatchUpdateStatementDsl<ID>(val table: Table, val idColumn: Column<ID>) : UpdateStatement(table, null) {
    /** The mappings of columns to update with their updated values for each entity in the batch. */
    val data = ArrayList<Pair<ID, Map<Column<*>, Any?>>>()
    override val firstDataSet: List<Pair<Column<*>, Any?>> get() = data.first().second.toList()

    /**
     * Adds the specified entity [id] to the current list of update statements, using the mapping of columns to update
     * provided for this `BatchUpdateStatement`.
     */
    fun addBatch(id: ID) {
        val lastBatch = data.lastOrNull()
        val different by lazy {
            val set1 = firstDataSet.map { it.first }.toSet()
            val set2 = lastBatch!!.second.keys
            (set1 - set2) + (set2 - set1)
        }

        if (data.size > 1 && different.isNotEmpty()) {
            throw BatchDataInconsistentException("Some values missing for batch update. Different columns: $different")
        }

        if (data.isNotEmpty()) {
            data[data.size - 1] = lastBatch!!.copy(second = values.toMap())
            values.clear()
            hasBatchedValues = true
        }
        data.add(id to values)
    }

    override fun <T, S : T?> update(column: Column<T>, value: Expression<S>) =
        error("Expressions unsupported in batch update")

    override fun prepareSQL(transaction: Transaction, prepared: Boolean): String {
        val updateSql = super.prepareSQL(transaction, prepared)
        val idEqCondition = "${transaction.identity(idColumn)} = ?"
        return "$updateSql WHERE $idEqCondition"
    }

    override fun PreparedStatementApi.executeInternal(transaction: Transaction): Int =
        if (data.size == 1) executeUpdate() else executeBatch().sum()

    override fun arguments(): Iterable<Iterable<Pair<IColumnType<*>, Any?>>> = data.map { (id, row) ->
        val idArg = listOf(idColumn.columnType to id)
        firstDataSet.map { it.first.columnType to row[it.first] } + idArg
    }
}