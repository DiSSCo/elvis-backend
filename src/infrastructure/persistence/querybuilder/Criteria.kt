@file:Suppress("MatchingDeclarationName", "unused")

package org.synthesis.infrastructure.persistence.querybuilder

import java.time.LocalDateTime
import java.util.*

@Suppress("TooManyFunctions")
class CriteriaBuilder {
    private var cursorPosition = 0
    private val patterns: MutableList<String> = mutableListOf()
    private val statements: MutableList<Any?> = mutableListOf()

    infix fun String.between(range: ClosedRange<LocalDateTime>) = add(
        "$this BETWEEN $${++cursorPosition} AND $${++cursorPosition}",
        listOf(range.start, range.endInclusive)
    )

    infix fun String.notBetween(range: ClosedRange<LocalDateTime>) = add(
        "$this NOT BETWEEN $${++cursorPosition} AND $${++cursorPosition}",
        listOf(range.start, range.endInclusive)
    )

    infix fun String.contains(values: List<Any?>) = add(
        "$this IN (${values.joinToString(", ") { "$${++cursorPosition}" }})",
        values
    )

    infix fun String.inArray(values: List<Any?>) = add(
        "$this @> '{${values.joinToString(", ")}}'"
    )

    infix fun String.notContains(values: List<Any?>) = add(
        "$this NOT IN (${values.joinToString(", ") { "$${++cursorPosition}" }})",
        values
    )

    infix fun String.eq(value: Any?) = if (value != null) {
        add("$this = $${++cursorPosition}", value)
    } else {
        add("$this IS NULL")
    }

    infix fun String.notEq(value: Any?) = if (value != null) {
        add("$this != $${++cursorPosition}", value)
    } else {
        add("$this IS NOT NULL")
    }

    infix fun String.gt(value: Number) = add("$this > $${++cursorPosition}", value)
    infix fun String.gte(value: Number) = add("$this >= $${++cursorPosition}", value)
    infix fun String.lt(value: Number) = add("$this < $${++cursorPosition}", value)
    infix fun String.lte(value: Number) = add("$this <= $${++cursorPosition}", value)
    infix fun String.like(value: String) = add("$this ILIKE $${++cursorPosition}", "%$value%")
    infix fun String.lLike(value: String) = add("$this ILIKE $${++cursorPosition}", "%$value")
    infix fun String.rLike(value: String) = add("$this ILIKE $${++cursorPosition}", "$value%")

    fun has() = patterns.isNotEmpty()
    fun build(): CompiledQuery {
        var result = ""

        var isFirstElement = true

        patterns.forEach {
            result += if (isFirstElement) it else " AND $it"
            isFirstElement = false
        }

        return CompiledQuery(sql = result, statements = statements)
    }

    @Suppress("ComplexMethod")
    private fun add(pattern: String, value: Any? = Unit) {
        patterns.add(pattern)

        when (value) {
            is List<*> -> value.forEach { statements.add(if (it is String) it.tryCastToUUID() else it) }
            is Set<*> -> value.forEach { statements.add(if (it is String) it.tryCastToUUID() else it) }
            is Unit -> Unit
            else -> statements.add(if (value is String) value.tryCastToUUID() else value)
        }
    }

    /**
     * fix error with casting string to UUID (pq driver-level)
     */
    private fun String.tryCastToUUID() = try {
        UUID.fromString(this)
    } catch (e: Exception) {
        // not interest
        this
    }
}

internal fun buildWhereSection(whereCriteria: CriteriaBuilder, orWhereCriteria: CriteriaBuilder): CompiledQuery {
    if (!whereCriteria.has() && orWhereCriteria.has()) {
        throw Exception("The OR section cannot be filled if the main condition section is empty")
    }

    if (!whereCriteria.has()) {
        return CompiledQuery(sql = "", statements = listOf())
    }

    val where = whereCriteria.build()
    val orWhere = orWhereCriteria.build()

    val statements: List<Any?> = where.statements.plus(orWhere.statements)
    var sql = " WHERE (${where.sql})"

    if (orWhere.sql.isNotEmpty()) {
        sql += " OR (${orWhere.sql})"
    }

    return CompiledQuery(sql = sql, statements = statements)
}
