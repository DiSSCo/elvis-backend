package org.synthesis.calls.request.comment

import java.util.*
import org.synthesis.account.UserAccountId
import org.synthesis.calls.request.CallRequestId
import org.synthesis.comment.*

interface CallRequestCommentsHandler {
    suspend fun handle(
        requestId: CallRequestId,
        authorId: UserAccountId,
        body: String,
        format: CommentFormat = CommentFormat.TEXT,
        replyTo: UUID?
    )
}

class ProxyCallRequestCommentsHandler(
    private val commentProvider: CommentProvider
) : CallRequestCommentsHandler {

    override suspend fun handle(
        requestId: CallRequestId,
        authorId: UserAccountId,
        body: String,
        format: CommentFormat,
        replyTo: UUID?
    ) {
        val threadId = CommentThreadId(requestId.uuid)
        val payload = CommentPayload(
            data = body,
            format = format
        )

        if (replyTo == null) {
            commentProvider.handle(
                CommentCommand.New(
                    threadId = threadId,
                    authorId = authorId,
                    payload = payload
                )
            )

            return
        }

        commentProvider.handle(
            CommentCommand.Reply(
                toMessage = CommentId(replyTo),
                authorId = authorId,
                payload = payload
            )
        )
    }
}
