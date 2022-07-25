package org.synthesis.calls.request.va.attachmnent

import java.time.LocalDateTime
import java.util.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.synthesis.account.UserAccountFinder
import org.synthesis.attachment.Attachment
import org.synthesis.attachment.AttachmentCollection
import org.synthesis.attachment.AttachmentId
import org.synthesis.attachment.AttachmentProvider
import org.synthesis.calls.request.CallRequestId
import org.synthesis.infrastructure.filesystem.FilesystemException
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.institution.InstitutionId

class VaCallRequestAttachmentPresenter(
    private val finder: VaCallRequestAttachmentFinder,
    private val attachmentProvider: AttachmentProvider,
    private val collection: AttachmentCollection,
    private val userAccountFinder: UserAccountFinder
) {
    /**
     * Retrieving a saved attachment
     *
     * @throws [StorageException.InteractingFailed]
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun find(callRequestId: CallRequestId, attachmentId: AttachmentId): CallRequestAttachmentResponse? =
        finder.find(callRequestId, attachmentId)?.hydrate()

    /**
     * Download attachment
     *
     * @throws [StorageException.InteractingFailed]
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun download(id: AttachmentId): Attachment? = attachmentProvider.receive(id, collection)

    /**
     * Receive a list of links to saved files in the context of the institute
     *
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun list(callRequestId: CallRequestId, institutionId: InstitutionId): List<CallRequestAttachmentResponse> =
        finder.findAll(callRequestId, institutionId).map { it.hydrate() }.toList()

    /**
     * Receive a list of links to saved files
     *
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun list(callRequestId: CallRequestId): List<CallRequestAttachmentResponse> =
        finder.findAll(callRequestId).map { it.hydrate() }.toList()

    private suspend fun VaCallRequestAttachment.hydrate(): CallRequestAttachmentResponse {
        val userAccount = userAccountFinder.find(owner.accountId)
            ?: error("Unable to find attachment author with id ${owner.accountId.uuid}")

        return CallRequestAttachmentResponse(
            id = attachmentId,
            owner = CallRequestAttachmentOwnerResponse(
                id = userAccount.id.uuid,
                fullName = userAccount.fullName.toString(),
                groups = userAccount.groups
            ),
            addedAt = addedAt,
            fileName = if (metadata.extension.isNotEmpty()) "$name.${metadata.extension}" else name
        )
    }
}

data class CallRequestAttachmentResponse(
    val id: AttachmentId,
    val owner: CallRequestAttachmentOwnerResponse,
    val fileName: String,
    val addedAt: LocalDateTime
)

data class CallRequestAttachmentOwnerResponse(
    val id: UUID,
    val fullName: String,
    val groups: List<String>
)
