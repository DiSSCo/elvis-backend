package org.synthesis.calls.store

import org.synthesis.calls.request.CallRequest
import org.synthesis.calls.request.CallRequestId
import org.synthesis.infrastructure.persistence.StorageException

interface CallRequestFinder<T : CallRequest> {
    /**
     * Gets the previously stored request by its ID.
     *
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(requestId: CallRequestId): T?
}
