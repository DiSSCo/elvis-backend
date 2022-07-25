package org.synthesis.attachment

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import java.time.LocalDateTime
import org.synthesis.infrastructure.persistence.*
import org.synthesis.infrastructure.persistence.querybuilder.*

interface AttachmentReferenceStore {

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(id: AttachmentId): AttachmentReference?

    /**
     * @throws [StorageException.UniqueConstraintViolationCheckFailed]
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun add(id: AttachmentId, filename: String, metadata: AttachmentMetadata)

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun remove(id: AttachmentId)
}

class PgAttachmentReferenceStore(
    private val sqlClient: SqlClient
) : AttachmentReferenceStore {

    override suspend fun add(id: AttachmentId, filename: String, metadata: AttachmentMetadata) {
        sqlClient.execute(
            insert(
                "attachments", mapOf(
                    "id" to id.uuid,
                    "file_name" to filename,
                    "extension" to metadata.extension,
                    "mime_type" to metadata.mimeType.base,
                    "mime_sub_type" to metadata.mimeType.subType,
                    "added_at" to LocalDateTime.now()
                )
            )
        )
    }

    override suspend fun remove(id: AttachmentId) {
        sqlClient.execute(
            delete("attachments") {
                where { "id" eq id.uuid }
            }
        )
    }

    override suspend fun find(id: AttachmentId): AttachmentReference? {
        return sqlClient.fetchOne(
            select("attachments") {
                where {
                    "id" eq id.uuid
                }
            }
        )?.hydrate()
    }

    private fun Row.hydrate() = AttachmentReference(
        id = AttachmentId(getUUID("id")),
        name = getString("file_name"),
        attachmentMetadata = AttachmentMetadata(
            extension = getString("extension"),
            mimeType = AttachmentMimeType(
                base = getString("mime_type"),
                subType = getString("mime_sub_type")
            )
        ),
        addedAt = getLocalDateTime("added_at")
    )
}
