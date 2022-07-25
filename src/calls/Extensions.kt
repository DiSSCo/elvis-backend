package org.synthesis.calls

import io.ktor.application.*
import org.synthesis.attachment.AttachmentId
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.ta.scoring.ScoreFormId
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveUuidFromParameters

/**
 * Extract call request id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.callId(): CallId = try {
    CallId(receiveUuidFromParameters("callId"))
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "callId",
        message = "Failed to retrieve call id"
    )
}

/**
 * Extract call request id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.callRequestId(): CallRequestId = try {
    CallRequestId(receiveUuidFromParameters("requestId"))
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "requestId",
        message = "Failed to retrieve request id"
    )
}
/**
 * Extract call request id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.scoreFormId(): ScoreFormId = try {
    ScoreFormId(receiveUuidFromParameters("scoreFormId"))
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "scoreFormId",
        message = "Failed to retrieve score form Id"
    )
}

/**
 * Extract extension id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.callRequestAttachmentId(): AttachmentId = try {
    AttachmentId(receiveUuidFromParameters("attachmentId"))
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "attachmentId",
        message = "Failed to retrieve attachment id"
    )
}
