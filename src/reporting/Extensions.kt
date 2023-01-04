package org.synthesis.reporting

import io.ktor.server.application.*
import org.synthesis.infrastructure.IncorrectRequestParameters

/**
 * Extract reporting call Id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.callId(): ReportingParameter = try {
    ReportingParameter(callId = parameters["callId"] ?: throw Exception())
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "callId",
        message = "Failed to retrieve reporting type"
    )
}

/**
 * Extract type from the URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.type(): ReportingParameter = try {
    ReportingParameter(type = parameters["type"] ?: throw Exception())
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "type",
        message = "Failed to retrieve facility id"
    )
}

/**
 * Extract type from the URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.group(): ReportingParameter = try {
    ReportingParameter(group = parameters["group"] ?: throw Exception())
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "group",
        message = "Failed to retrieve facility id"
    )
}

/**
 * Extract type from the URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.role(): ReportingParameter = try {
    ReportingParameter(group = parameters["role"] ?: throw Exception())
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "role",
        message = "Failed to retrieve facility id"
    )
}
