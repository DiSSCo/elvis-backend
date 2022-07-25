package org.synthesis.infrastructure

abstract class ApplicationException(message: String?) : Exception(message)

class IncorrectRequestParameters(val violations: Map<String, String>) : ApplicationException("Incorrect request data") {
    companion object Factory {
        fun create(field: String, message: String) = IncorrectRequestParameters(mapOf(field to message))
    }
}
