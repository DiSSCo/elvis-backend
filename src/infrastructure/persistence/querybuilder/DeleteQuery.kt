package org.synthesis.infrastructure.persistence.querybuilder

class DeleteQuery(
    private val tableName: String
) : Query {
    private val whereCriteria = CriteriaBuilder()
    private val orWhereCriteria = CriteriaBuilder()

    init {
        if (tableName.isBlank()) {
            throw QueryGenerationFailed("Table name must be specified")
        }
    }

    fun where(code: CriteriaBuilder.() -> Unit) = whereCriteria.apply(code)
    fun orWhere(code: CriteriaBuilder.() -> Unit) = orWhereCriteria.apply(code)

    override fun compile(): CompiledQuery {
        val where = buildWhereSection(whereCriteria, orWhereCriteria)

        return CompiledQuery(
            sql = "DELETE FROM ${tableName}${where.sql};",
            statements = where.statements
        )
    }
}
