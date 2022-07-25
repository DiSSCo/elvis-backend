@file:Suppress("MemberVisibilityCanBePrivate")

package org.synthesis.infrastructure.persistence.querybuilder

class SelectQuery(
    private val tableName: String,
    private val columns: List<String>
) : Query {
    var limit: Int? = null
    var offset: Int? = null
    var distinct: Boolean = false

    private var orderBy: Pair<String, String>? = null
    private val whereCriteria = CriteriaBuilder()
    private val orWhereCriteria = CriteriaBuilder()
    private val groupBy: MutableList<String> = mutableListOf()
    private val joins: MutableList<String> = mutableListOf()

    init {
        if (tableName.isBlank()) {
            throw QueryGenerationFailed("Table name must be specified")
        }
    }

    fun groupBy(vararg cols: String) = groupBy.addAll(cols)

    fun where(code: CriteriaBuilder.() -> Unit) = whereCriteria.apply(code)
    fun orWhere(code: CriteriaBuilder.() -> Unit) = orWhereCriteria.apply(code)

    infix fun String.orderBy(direction: String) {
        orderBy = Pair(this, direction.toUpperCase())
    }

    infix fun String.innerJoin(withExpression: String) = joins.add("INNER JOIN $this ON $withExpression")
    infix fun String.leftJoin(withExpression: String) = joins.add("LEFT JOIN $this ON $withExpression")
    infix fun String.rightJoin(withExpression: String) = joins.add("RIGHT JOIN $this ON $withExpression")
    infix fun String.fullJoin(withExpression: String) = joins.add("FULL JOIN $this ON $withExpression")

    fun leftJoinOld(table: String, expression: String) = joins.add("LEFT JOIN $table ON $expression")

    override fun compile(): CompiledQuery {
        var sql = "SELECT${if (distinct) " DISTINCT" else ""} ${columns.joinToString(", ")} FROM $tableName"

        val currentOrder = orderBy
        val where = buildWhereSection(whereCriteria, orWhereCriteria)

        joins.forEach { sql += " $it" }

        sql += where.sql

        if (groupBy.isNotEmpty()) {
            sql += " GROUP BY ${groupBy.joinToString(",")}"
        }

        if (currentOrder != null) {
            sql += " ORDER BY ${currentOrder.first} ${currentOrder.second.toUpperCase()}"
        }

        if (limit != null) sql += " LIMIT $limit"
        if (offset != null) sql += " OFFSET $offset"

        return CompiledQuery(
            sql = "${sql.trim()};",
            statements = where.statements
        )
    }
}
