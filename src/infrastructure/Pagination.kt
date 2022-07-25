package org.synthesis.infrastructure

import kotlin.math.max

typealias PaginationBuilder = (total: Int, max: Int) -> Pagination

class Paginator<T>(
    private val perPage: Int,
    private val fetch: suspend (range: IntRange, PaginationBuilder) -> T
) {
    /**
     * Returns a page using [fetch] lambda
     */
    @Throws(AssertionError::class)
    suspend fun page(pageIndex: Int): T {
        assert(pageIndex > 0) { "Page must be positive" }

        val begin = max(0, (pageIndex - 1) * perPage)
        val end = pageIndex * perPage - 1
        return fetch(IntRange(begin, end)) { total, max ->
            Pagination(
                perPage = perPage,
                page = pageIndex,
                total = total,
                max = max
            )
        }
    }
}

data class Pagination(val page: Int, val perPage: Int, val total: Int, val max: Int = total)

data class Page<T>(
    val rows: List<T>,
    val pagination: Pagination
)

val IntRange.size get() = endInclusive - start + 1
