package org.synthesis.infrastructure.persistence.querybuilder

class InsertQuery(
    private val tableName: String,
    private val rows: Map<String, Any?>
) : Query {

    private var onConflict: CompiledQuery? = null

    init {

        if (tableName.isBlank()) {
            throw QueryGenerationFailed("Table name must be specified")
        }

        if (rows.isEmpty()) {
            throw QueryGenerationFailed("Data set cant be empty")
        }
    }

    fun onConflict(constraint: String, action: OnConflict) = onConflict(ConflictTarget.Constraint(constraint), action)
    fun onConflict(columns: List<String>, action: OnConflict) = onConflict(ConflictTarget.Columns(columns), action)

    override fun compile(): CompiledQuery {
        var cursorPosition = 0
        val placeHolders = rows.keys.joinToString(", ") { "$${++cursorPosition}" }

        var statements = rows.values.toList()
        var sql = "INSERT INTO $tableName (${rows.keys.joinToString(", ")}) VALUES ($placeHolders)"

        val currentOnComplete = onConflict

        if (currentOnComplete != null) {
            sql += " ${currentOnComplete.sql}"
            statements = statements.plus(currentOnComplete.statements)
        }

        return CompiledQuery(
            sql = "${sql.trim()};",
            statements = statements
        )
    }

    private fun onConflict(target: ConflictTarget, action: OnConflict) {
        var cursorPosition = rows.count()
        val targetSql = "ON CONFLICT " + when (target) {
            is ConflictTarget.Constraint -> "ON CONSTRAINT"
            is ConflictTarget.Columns -> "(${target.collection.joinToString(", ")})"
        }

        val sql = "$targetSql DO " + when (action) {
            is OnConflict.DoNothing -> "NOTHING"
            is OnConflict.DoUpdate -> "UPDATE SET " + action.rows.map { (field, _) ->
                "$field = $${++cursorPosition}"
            }.joinToString(", ")
        }

        onConflict = CompiledQuery(
            sql = sql,
            statements = when (action) {
                is OnConflict.DoNothing -> listOf()
                is OnConflict.DoUpdate -> action.rows.values.toList()
            }
        )
    }
}

sealed class OnConflict {
    data class DoNothing(val stub: Unit = Unit) : OnConflict()
    data class DoUpdate(val rows: Map<String, Any?>) : OnConflict()
}

private sealed class ConflictTarget {
    data class Constraint(val name: String) : ConflictTarget()
    data class Columns(val collection: List<String>) : ConflictTarget()
}
