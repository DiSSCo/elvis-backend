package org.synthesis.calls.request.va.attachmnent

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.synthesis.account.UserAccountId
import org.synthesis.attachment.*
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.attachment.CallRequestAttachmentId
import org.synthesis.calls.request.attachment.CallRequestAttachmentOwner
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.infrastructure.persistence.querybuilder.fetchAll
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select
import org.synthesis.institution.InstitutionId

interface VaCallRequestAttachmentFinder {
    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(callRequestId: CallRequestId, attachmentId: AttachmentId): VaCallRequestAttachment?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    fun findAll(callRequestId: CallRequestId, institutionId: InstitutionId): Flow<VaCallRequestAttachment>

    /**
     * @throws [StorageException.InteractingFailed]
     */
    fun findAll(callRequestId: CallRequestId): Flow<VaCallRequestAttachment>
}

class DefaultVaCallRequestAttachmentFinder(
    private val sqlClient: SqlClient
) : VaCallRequestAttachmentFinder {
    private val requestAttachmentTable = "requests_attachments AS cra"
    private val attachmentTable = "attachments AS a"
    private val storedFileIdMapping = "cra.stored_file_id = a.id"
    private val callRequestIdField = "cra.call_request_id"

    override suspend fun find(callRequestId: CallRequestId, attachmentId: AttachmentId): VaCallRequestAttachment? {
        val query = select(requestAttachmentTable, listOf("cra.*", "a.*")) {
            attachmentTable innerJoin storedFileIdMapping
            where {
                callRequestIdField eq callRequestId.uuid
                "cra.stored_file_id" eq attachmentId.uuid
            }
        }

        return sqlClient.fetchOne(query)?.hydrate()
    }

    override fun findAll(
        callRequestId: CallRequestId,
        institutionId: InstitutionId
    ): Flow<VaCallRequestAttachment> {
        val query = select(requestAttachmentTable, listOf("cra.*", "a.*")) {
            attachmentTable innerJoin storedFileIdMapping
            where {
                callRequestIdField eq callRequestId.uuid
                "cra.institution_id" eq institutionId.grid.value
            }
        }

        return sqlClient.fetchAll(query).map { it.hydrate() }
    }

    override fun findAll(callRequestId: CallRequestId): Flow<VaCallRequestAttachment> {
        val query = select(requestAttachmentTable, listOf("cra.*", "a.*")) {
            attachmentTable innerJoin storedFileIdMapping
            where {
                callRequestIdField eq callRequestId.uuid
            }
        }

        return sqlClient.fetchAll(query).map { it.hydrate() }
    }

    private fun Row.hydrate() = VaCallRequestAttachment(
        id = CallRequestAttachmentId(getUUID("id")),
        attachmentId = AttachmentId(getUUID("stored_file_id")),
        callRequestId = CallRequestId(getUUID("call_request_id")),
        institutionId = if (!getString("institution_id").isNullOrBlank()) {
            InstitutionId.fromString(getString("institution_id"))
        } else {
            null
        },
        addedAt = getLocalDateTime("created_at"),
        name = getString("file_name"),
        metadata = AttachmentMetadata(
            extension = getString("extension"),
            mimeType = AttachmentMimeType(
                base = getString("mime_type"),
                subType = getString("mime_sub_type")
            )
        ),
        owner = CallRequestAttachmentOwner(
            accountId = UserAccountId(getUUID("owner_id"))
        )
    )
}
