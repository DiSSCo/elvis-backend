package org.synthesis.calls.request.va.attachmnent

import io.vertx.sqlclient.SqlClient
import java.time.LocalDateTime
import java.util.*
import org.synthesis.attachment.AttachmentCollection
import org.synthesis.attachment.AttachmentId
import org.synthesis.attachment.AttachmentMetadata
import org.synthesis.attachment.AttachmentProvider
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.attachment.CallRequestAttachmentOwner
import org.synthesis.infrastructure.filesystem.FilesystemException
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.infrastructure.persistence.querybuilder.delete
import org.synthesis.infrastructure.persistence.querybuilder.execute
import org.synthesis.infrastructure.persistence.querybuilder.insert
import org.synthesis.institution.InstitutionId

interface VaCallRequestAttachmentStore {
    /**
     * @throws [StorageException.InteractingFailed]
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun add(
        callRequestId: CallRequestId,
        institutionId: InstitutionId,
        user: CallRequestAttachmentOwner,
        fileName: String,
        payload: ByteArray,
        metadata: AttachmentMetadata
    ): AttachmentId

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun remove(callRequestId: CallRequestId, attachmentId: AttachmentId)
}

class DefaultVaCallRequestAttachmentStore(
    private val sqlClient: SqlClient,
    private val attachmentProvider: AttachmentProvider,
    private val collection: AttachmentCollection
) : VaCallRequestAttachmentStore {

    override suspend fun add(
        callRequestId: CallRequestId,
        institutionId: InstitutionId,
        user: CallRequestAttachmentOwner,
        fileName: String,
        payload: ByteArray,
        metadata: AttachmentMetadata
    ): AttachmentId {
        val attachmentId = attachmentProvider.store(fileName, payload, metadata, collection)

        try {
            sqlClient.execute(
                insert(
                    "requests_attachments", mapOf(
                        "id" to UUID.randomUUID(),
                        "call_request_id" to callRequestId.uuid,
                        "institution_id" to institutionId.grid.value,
                        "stored_file_id" to attachmentId.uuid,
                        "owner_id" to user.accountId.uuid,
                        "created_at" to LocalDateTime.now()
                    )
                )
            )

            return attachmentId
        } catch (e: Exception) {
            attachmentProvider.remove(attachmentId, collection)

            throw e
        }
    }

    override suspend fun remove(callRequestId: CallRequestId, attachmentId: AttachmentId) = try {
        attachmentProvider.remove(attachmentId, collection)
    } finally {
        sqlClient.execute(
            delete("requests_attachments") {
                where {
                    "call_request_id" eq callRequestId.uuid
                    "stored_file_id" eq attachmentId.uuid
                }
            }
        )
    }
}
