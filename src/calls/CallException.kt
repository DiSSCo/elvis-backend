package org.synthesis.calls

import org.synthesis.calls.request.CallRequestId
import org.synthesis.comment.CommentThreadId
import org.synthesis.infrastructure.ApplicationException

sealed class CallException(message: String) : ApplicationException(message) {

    class CallNotFound(message: String) : CallException(message)

    class FlowNotImplemented(id: CallId, type: CallType) :
        CallException("Flow for Call with id `${id.uuid}` was not implemented (type: $type)")

    class CallRequestAlreadyAdded(id: CallRequestId) :
        CallException("Request with ID `${id.uuid}` already exists")

    class CallRequestNotFound(id: CallRequestId) : CallException("Call request with id `${id.uuid}` was not found")

    class CallClosedForRequest(id: CallId) : CallException("Call with id `${id.uuid}` is not open for request")

    class NoCommentThread(commentThreadId: CommentThreadId) :
        CallException("Comment thread with id : ${commentThreadId.uuid} not found.")
}
