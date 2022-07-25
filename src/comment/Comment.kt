package org.synthesis.comment

import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDateTime
import java.util.*
import org.synthesis.account.UserAccountId

data class CommentThreadId(
    @JsonValue
    val uuid: UUID
) {
    companion object {
        fun next(): CommentThreadId = CommentThreadId(UUID.randomUUID())
    }
}

data class CommentId(
    @JsonValue
    val uuid: UUID
) {
    companion object {
        fun next(): CommentId = CommentId(UUID.randomUUID())
    }
}

data class CommentThread(
    val id: CommentThreadId,
    val messages: List<Comment>,
    val createdAt: LocalDateTime,
    val deletedAt: LocalDateTime?
)

data class Comment(
    val id: CommentId,
    val authorId: UserAccountId,
    val threadId: CommentThreadId,
    val replyTo: CommentId? = null,
    val payload: CommentPayload,
    val createdAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null
)

enum class CommentFormat {
    TEXT,
    HTML,
    MARKDOWN
}

data class CommentPayload(
    val format: CommentFormat = CommentFormat.TEXT,
    val data: String
)

fun CommentPayload.escapedPayload() = data.escapeString()

fun String.escapeString() = this.replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&#39;")
