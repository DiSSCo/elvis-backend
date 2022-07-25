package org.synthesis.infrastructure.persistence.querybuilder

/**
 * Usage example:
 *
 * val query = select("call_request cr", listOf("cr.*", "cra.*")) {
 *    limit = 100
 *    offset = 0
 *
 *    where {
 *        "cr.requester_id" eq userId
 *    }
 *
 *    orWhere {
 *        "cr.status" contains listOf("submitted", "draft")
 *        "cr.requester_id" notEq userId
 *    }
 *
 *    "cr.age" orderBy "ASC"
 *    "call_request_institutions cra" leftJoin "cra.form_id = a.id"
 * }
 */
fun select(
    from: String,
    columns: List<String> = listOf("*"),
    code: (SelectQuery.() -> Unit)? = null
): CompiledQuery {
    val query = SelectQuery(from, columns)

    if (code != null) query.apply(code)

    return query.compile()
}

/**
 * Usage example:
 *
 * val query = insert("table", mapOf("field" to "value", "another_field" to 3.14, "created_at" to null)) {
 *     onConflict(
 *        listOf("field"),
 *        OnConflict.DoUpdate(mapOf("field" to "anotherValue", "created_at" to currentDate))
 *     )
 * }
 */
fun insert(to: String, rows: Map<String, Any?>, code: (InsertQuery.() -> Unit)? = null): CompiledQuery {
    val query = InsertQuery(to, rows)

    if (code != null) query.apply(code)

    return query.compile()
}

/**
 * Usage example:
 *
 * val query = update("table_name", mapOf("title" to "newTitle", "score" to 100)) {
 *     where {
 *         "id" eq uuid
 *     }
 * }
 */
fun update(on: String, rows: Map<String, Any?>, code: (UpdateQuery.() -> Unit)): CompiledQuery {
    val query = UpdateQuery(on, rows)

    query.apply(code)

    return query.compile()
}

fun delete(from: String, code: (DeleteQuery.() -> Unit)): CompiledQuery {
    val query = DeleteQuery(from)

    query.apply(code)

    return query.compile()
}
