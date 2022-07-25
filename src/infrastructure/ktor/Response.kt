package org.synthesis.infrastructure.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import org.synthesis.infrastructure.ApiResponse
import org.synthesis.infrastructure.Paginated

suspend fun ApplicationCall.respondSuccess() = respond(
    HttpStatusCode.OK,
    ApiResponse.Success(
        data = null
    )
)

suspend fun <T> ApplicationCall.respondSuccess(data: T? = null) = respond(
    HttpStatusCode.OK,
    ApiResponse.Success(
        data = data
    )
)

suspend fun ApplicationCall.respondAttachment(fileName: String, stream: suspend ByteWriteChannel.() -> Unit) {
    response.header(
        HttpHeaders.ContentDisposition,
        ContentDisposition.Attachment.withParameter(
            ContentDisposition.Parameters.FileName, fileName
        ).toString()
    )

    respond(object : OutgoingContent.WriteChannelContent() {
        override suspend fun writeTo(channel: ByteWriteChannel) {
            channel.stream()
        }
    })
}

suspend fun <T> ApplicationCall.respondSuccess(description: String? = null, data: T? = null) = respond(
    HttpStatusCode.OK,
    ApiResponse.Success(
        description = description,
        data = data
    )
)

suspend fun ApplicationCall.respondSuccess(description: String) = respond(
    HttpStatusCode.OK,
    ApiResponse.Success(
        description = description,
        data = null
    )
)

suspend fun <T> ApplicationCall.respondCreated(description: String? = null, data: T? = null) = respond(
    HttpStatusCode.Created,
    ApiResponse.Success(
        description = description,
        data = data
    )
)

suspend fun ApplicationCall.respondCreated(description: String) = respond(
    HttpStatusCode.Created,
    ApiResponse.Success(
        description = description,
        data = null
    )
)

suspend fun <T> ApplicationCall.respondBadRequest(description: String? = null, violations: T? = null) = respond(
    HttpStatusCode.BadRequest,
    ApiResponse.Success(
        code = ApiResponse.Code.INCORRECT_PARAMETERS,
        description = description,
        data = violations
    )
)

suspend fun <T> ApplicationCall.respondBadRequest(violations: T? = null) = respond(
    HttpStatusCode.BadRequest,
    ApiResponse.Success(
        code = ApiResponse.Code.INCORRECT_PARAMETERS,
        description = "Incorrect request parameters",
        data = violations
    )
)

suspend fun <T> ApplicationCall.respondCollection(list: List<T>) = respond(
    ApiResponse.Success(
        data = Paginated(
            total = list.size,
            rows = list
        )
    )
)

suspend fun PipelineContext<*, ApplicationCall>.accessDenied() = call.respond(
    HttpStatusCode.Forbidden,
    ApiResponse.Fail(
        code = ApiResponse.Code.ACCESS_DENIED,
        description = "Access denied"
    )
)
