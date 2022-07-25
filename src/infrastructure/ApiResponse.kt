package org.synthesis.infrastructure

sealed class ApiResponse {
    enum class Code {
        SUCCESS,
        INCORRECT_PARAMETERS,
        ACCESS_DENIED,
        INTERNAL_ERROR
    }

    data class Success<T>(
        val code: Code = Code.SUCCESS,
        val description: String? = null,
        val data: T? = null
    ) : ApiResponse()

    data class Fail(
        val code: Code = Code.INTERNAL_ERROR,
        val description: String? = null
    ) : ApiResponse()
}

data class Paginated<T>(
    val total: Int,
    val rows: List<T>
)
