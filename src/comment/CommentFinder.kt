package org.synthesis.comment

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.synthesis.account.UserAccountId
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.infrastructure.persistence.querybuilder.fetchAll
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select

interface CommentFinder {
    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(id: CommentId): Comment?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(id: CommentThreadId): CommentThread?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun findAll(inThread: CommentThreadId): Flow<Comment>
}

class PgCommentFinder(
    private val sqlClient: SqlClient
) : CommentFinder {

    override suspend fun find(id: CommentId): Comment? {
        val query = select("comments") {
            where { "id" eq id.uuid }
        }

        return sqlClient.fetchOne(query)?.mapComment()
    }

    override suspend fun find(id: CommentThreadId): CommentThread? {
        val query = select("comments_thread") {
            where { "id" eq id.uuid }
        }

        return sqlClient.fetchOne(query)?.mapThread()
    }

    override suspend fun findAll(inThread: CommentThreadId): Flow<Comment> {
        val query = select("comments") {
            where { "thread_id" eq inThread.uuid }

            "created_at" orderBy "DESC"
        }

        return sqlClient.fetchAll(query).map { it.mapComment() }
    }

    private suspend fun Row.mapThread(): CommentThread {
        val id = CommentThreadId(getUUID("id"))

        return CommentThread(
            id = id,
            messages = findAll(id).toList(),
            createdAt = getLocalDateTime("created_at"),
            deletedAt = getLocalDateTime("deleted_at")
        )
    }

    private fun Row.mapComment() = Comment(
        id = CommentId(getUUID("id")),
        authorId = UserAccountId(getUUID("author_id")),
        threadId = CommentThreadId(getUUID("thread_id")),
        payload = CommentPayload(
            format = CommentFormat.valueOf(getString("format").uppercase()),
            data = getString("message").escapeString()
        ),
        createdAt = getLocalDateTime("created_at"),
        deletedAt = getLocalDateTime("deleted_at"),
        replyTo = if (getValue("reply_to") != null) CommentId(getUUID("reply_to")) else null
    )
}
