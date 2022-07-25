package org.synthesis.calls.store

import org.synthesis.account.UserAccountId
import org.synthesis.calls.request.CallRequest
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.institution.InstitutionId

interface CallRequestStore<T : CallRequest> {

    /**
     * Add a new request
     *
     * @throws [StorageException.UniqueConstraintViolationCheckFailed]
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun add(request: T)

    /**
     * Update existing request
     *
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun save(request: T)
    suspend fun updateCoordinator(coordinatorId: UserAccountId, institutionId: InstitutionId)
}
