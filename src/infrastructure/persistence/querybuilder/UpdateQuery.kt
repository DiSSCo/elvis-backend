package org.synthesis.infrastructure.persistence.querybuilder

class UpdateQuery(
    private val tableName: String,
    private val rows: Map<String, Any?>
) : Query {
    private val whereCriteria = CriteriaBuilder()
    private val orWhereCriteria = CriteriaBuilder()

    init {
        if (tableName.isBlank()) {
            throw QueryGenerationFailed("Table name must be specified")
        }

        if (rows.isEmpty()) {
            throw QueryGenerationFailed("Update rows cant be empty")
        }
    }

    fun where(code: CriteriaBuilder.() -> Unit) = whereCriteria.apply(code)
    fun orWhere(code: CriteriaBuilder.() -> Unit) = orWhereCriteria.apply(code)

    override fun compile(): CompiledQuery {
        val where = buildWhereSection(whereCriteria, orWhereCriteria)
        val statements = where.statements
        var cursorPosition = statements.count()

        val updatedFields = rows.map { (column, _) -> "$column = $${++cursorPosition}" }.joinToString(", ")

        return CompiledQuery(
            sql = "UPDATE $tableName SET $updatedFields ${where.sql};",
            statements = statements.plus(rows.values)
        )
    }
}
