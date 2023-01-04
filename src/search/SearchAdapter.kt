package org.synthesis.search

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlin.math.min
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.synthesis.infrastructure.Page
import org.synthesis.infrastructure.Paginator
import org.synthesis.infrastructure.persistence.querybuilder.*

private data class SearchResult<T>(
    val rows: List<T>,
    val total: Int
)

interface SearchAdapter<T> {
    suspend fun handle(request: SearchRequest): Page<T>
}

class PostgreSqlSearchAdapter<T>(
    private val sqlClient: SqlClient,
    private val table: String,
    private val transformer: (Row) -> T
) : SearchAdapter<T> {
    override suspend fun handle(request: SearchRequest): Page<T> {
        val paginator = Paginator(request.perPage) { range, pagination ->
            val searchResult = search(request, range)

            Page(
                pagination = pagination(searchResult.total, min(searchResult.total, 100)),
                rows = searchResult.rows
            )
        }

        return paginator.page(request.page)
    }

    private suspend fun search(request: SearchRequest, range: IntRange): SearchResult<T> = coroutineScope {
        val recordCount = async {
            val query = createQuery(request, null, listOf("COUNT(id) as total"))
            val totalRowsResult = sqlClient.fetchOne(query)

            totalRowsResult?.getInteger("total") ?: 0
        }

        val records = async {
            sqlClient.fetchAll(createQuery(request, range)).map { transformer(it) }
        }

        SearchResult(
            total = recordCount.await(),
            rows = records.await().toList()
        )
    }

    @Suppress("ComplexMethod")
    private fun createQuery(
        request: SearchRequest,
        range: IntRange?,
        columns: List<String> = listOf("*")
    ): CompiledQuery {
        val whereCriteria: MutableList<SearchCriteria> = mutableListOf()
        val orWhereCriteria: MutableList<SearchCriteria> = mutableListOf()

        request.queries.forEach { query ->
            query.criteria.forEach { searchCriteria ->
                if (query.type.lowercase() == "and") {
                    whereCriteria.add(searchCriteria)
                } else {
                    orWhereCriteria.add(searchCriteria)
                }
            }
        }

        return select(table, columns) {
            if (request.orderBy != null) {
                request.orderBy.field orderBy request.orderBy.direction
            }

            if (whereCriteria.isNotEmpty()) {
                where { whereCriteria.forEach { this.add(it) } }
            }

            if (orWhereCriteria.isNotEmpty()) {
                orWhere { orWhereCriteria.forEach { this.add(it) } }
            }

            if (range != null) {
                limit = range.last - range.first + 1
                offset = range.first
            }
        }
    }
}

@Suppress("UnsafeCast")
private fun CriteriaBuilder.add(criteria: SearchCriteria) {
    when (criteria.type) {
        "eq" -> criteria.field eq criteria.value
        "notEq" -> criteria.field notEq criteria.value
        "in" -> criteria.field contains criteria.value as List<Any?>
        "contains" -> criteria.field inArray criteria.value as List<Any?>
        "notIn" -> criteria.field notContains criteria.value as List<Any?>
        "gt" -> criteria.field gt criteria.value as Number
        "gte" -> criteria.field gte criteria.value as Number
        "lt" -> criteria.field lt criteria.value as Number
        "lte" -> criteria.field lte criteria.value as Number
        "like" -> criteria.field like criteria.value as String
        "rLike" -> criteria.field rLike criteria.value as String
        "lLike" -> criteria.field lLike criteria.value as String
    }
}
