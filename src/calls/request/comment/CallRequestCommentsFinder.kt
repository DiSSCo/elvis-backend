package org.synthesis.calls.request.comment

import kotlinx.coroutines.flow.Flow
import org.synthesis.calls.request.CallRequestId
import org.synthesis.comment.*
import org.synthesis.infrastructure.persistence.StorageException

interface CallRequestCommentsFinder {

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(id: CallRequestId): CommentThread?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun findAll(id: CallRequestId): Flow<Comment>
}

class ProxyCallRequestCommentsFinder(
    private val finder: CommentFinder
) : CallRequestCommentsFinder {

    override suspend fun find(id: CallRequestId): CommentThread? = finder.find(id.asThread())

    override suspend fun findAll(id: CallRequestId): Flow<Comment> = finder.findAll(id.asThread())

    private fun CallRequestId.asThread() = CommentThreadId(uuid)
}
