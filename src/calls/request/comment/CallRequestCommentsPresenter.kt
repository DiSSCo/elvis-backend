package org.synthesis.calls.request.comment

import java.time.LocalDateTime
import java.util.*
import org.synthesis.account.UserAccountFinder
import org.synthesis.calls.request.CallRequestId
import org.synthesis.comment.Comment
import org.synthesis.comment.CommentId
import org.synthesis.comment.CommentPayload

class CallRequestCommentsPresenter(
    private val callRequestCommentsFinder: CallRequestCommentsFinder,
    private val userAccountFinder: UserAccountFinder
) {
    suspend fun find(requestId: CallRequestId): RequestCallCommentThreadResponse {
        val commentThread = callRequestCommentsFinder.find(requestId)

        if (commentThread != null) {
            return RequestCallCommentThreadResponse(
                requestId = CallRequestId(commentThread.id.uuid),
                createdAt = commentThread.createdAt,
                deletedAt = commentThread.deletedAt,
                messages = commentThread.messages.map { it.toResponse() }
            )
        }

        /** @todo: more pretty design */
        return RequestCallCommentThreadResponse(
            requestId = requestId,
            createdAt = LocalDateTime.now(),
            deletedAt = null,
            messages = listOf()
        )
    }

    private suspend fun Comment.toResponse(): RequestCallCommentResponse {
        val user = userAccountFinder.find(authorId)

        return RequestCallCommentResponse(
            id = id,
            author = CommentAuthor(
                id = authorId.uuid,
                fullName = user?.fullName?.toString() ?: "",
                groups = user?.groups ?: listOf()
            ),
            replyTo = replyTo,
            payload = payload,
            createdAt = createdAt,
            deletedAt = deletedAt
        )
    }
}

data class RequestCallCommentThreadResponse(
    val requestId: CallRequestId,
    val messages: List<RequestCallCommentResponse>,
    val createdAt: LocalDateTime,
    val deletedAt: LocalDateTime?
)

data class RequestCallCommentResponse(
    val id: CommentId,
    val author: CommentAuthor,
    val replyTo: CommentId?,
    val payload: CommentPayload,
    val createdAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null
)

data class CommentAuthor(
    val id: UUID,
    val fullName: String,
    val groups: List<String>
)
