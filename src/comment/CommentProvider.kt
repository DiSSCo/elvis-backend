package org.synthesis.comment

import java.time.LocalDateTime
import org.synthesis.account.UserAccountId
import org.synthesis.infrastructure.persistence.*

sealed class CommentCommand {
    data class New(
        val threadId: CommentThreadId,
        val authorId: UserAccountId,
        val payload: CommentPayload
    ) : CommentCommand()

    data class Reply(
        val toMessage: CommentId,
        val authorId: UserAccountId,
        val payload: CommentPayload
    )
}

object OriginalMessageNotFound : Exception()

class CommentProvider(
    private val store: CommentStore,
    private val finder: CommentFinder
) {

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun handle(command: CommentCommand.New) {
        val comment = Comment(
            id = CommentId.next(),
            threadId = command.threadId,
            authorId = command.authorId,
            payload = command.payload,
            createdAt = LocalDateTime.now()
        )

        store.add(comment)
    }

    /**
     * @throws [StorageException.InteractingFailed]
     * @throws [OriginalMessageNotFound]
     */
    suspend fun handle(command: CommentCommand.Reply) {
        val originalComment = finder.find(command.toMessage) ?: throw OriginalMessageNotFound

        val comment = Comment(
            id = CommentId.next(),
            threadId = originalComment.threadId,
            authorId = command.authorId,
            payload = command.payload,
            createdAt = LocalDateTime.now()
        )

        store.add(comment)
    }
}
