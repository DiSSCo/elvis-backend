package org.synthesis.calls.request.va.export.task

import org.synthesis.account.UserAccountFinder
import org.synthesis.calls.CallException
import org.synthesis.calls.request.va.VaCallRequest
import org.synthesis.comment.CommentFinder
import org.synthesis.comment.CommentThread
import org.synthesis.comment.CommentThreadId
import org.synthesis.comment.escapedPayload

class VaCallRequestCommentsOverview(
    private val commentFinder: CommentFinder,
    private val userAccountFinder: UserAccountFinder
) : ExportTask {

    /**
     * @throws [CallException.NoCommentThread]
     */
    override suspend fun generate(callRequest: VaCallRequest): String {
        val threadId = CommentThreadId(callRequest.id().uuid)
        val thread = commentFinder.find(threadId) ?: throw CallException.NoCommentThread(threadId)

        return thread.makeHtml()
    }

    private suspend fun CommentThread.makeHtml(): String {
        val commentBody = StringBuilder()

        messages.forEach {
            val author = userAccountFinder.find(it.authorId)

            commentBody.append(
                """
            <div class="user-name">${author?.fullName?.toString()}</div>
            <div class="time">${it.createdAt}</div>
            <div class="comment">${it.payload.escapedPayload()}</div>
            """.trimIndent()
            )
        }
        return """ 
            <body>
            <h1>Comments export for VA request ${id.uuid}</h1>
            $commentBody
            </body>
           """.trimIndent()
    }
}
