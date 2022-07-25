package org.synthesis.comment

import io.vertx.pgclient.PgPool
import java.time.LocalDateTime
import org.synthesis.infrastructure.persistence.*
import org.synthesis.infrastructure.persistence.querybuilder.*

interface CommentStore {

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun add(comment: Comment)
}

class PgCommentStore(
    private val sqlClient: PgPool
) : CommentStore {

    override suspend fun add(comment: Comment) {
        sqlClient.transaction {
            execute(
                insert(
                    "comments_thread", mapOf(
                        "id" to comment.threadId.uuid,
                        "created_at" to LocalDateTime.now()
                    )
                ) {
                    onConflict(
                        columns = listOf("id"),
                        action = OnConflict.DoNothing()
                    )
                }
            )

            execute(
                insert(
                    "comments", mapOf(
                        "id" to CommentId.next().uuid,
                        "thread_id" to comment.threadId.uuid,
                        "author_id" to comment.authorId.uuid,
                        "reply_to" to if (comment.replyTo != null) comment.replyTo.uuid else null,
                        "message" to comment.payload.data,
                        "format" to comment.payload.format.toString().toLowerCase(),
                        "created_at" to LocalDateTime.now()
                    )
                )
            )
        }
    }
}
