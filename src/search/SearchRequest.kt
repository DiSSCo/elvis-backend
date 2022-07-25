package org.synthesis.search

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Positive

data class SearchCriteria(
    @NotEmpty
    @NotBlank
    val field: String,
    @NotEmpty
    @NotBlank
    val type: String,
    val value: Any?
)

data class OrderBy(
    @NotEmpty
    @NotBlank
    val field: String,
    @NotEmpty
    @NotBlank
    val direction: String
)

data class SearchQuery(
    @NotEmpty
    @NotBlank
    val type: String,
    @NotEmpty
    val criteria: List<SearchCriteria>
)

data class SearchRequest(
    @NotEmpty
    @NotBlank
    val index: String,
    val queries: List<SearchQuery>,
    val orderBy: OrderBy?,
    @Positive
    val page: Int = 1,
    @Positive
    val perPage: Int = 100
)
