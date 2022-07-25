package org.synthesis.attachment

import org.synthesis.infrastructure.filesystem.*
import org.synthesis.infrastructure.persistence.StorageException

class AttachmentProvider(
    private val store: AttachmentReferenceStore,
    private val filesystem: Filesystem
) {
    /**
     * Store a new attachment
     *
     * Note: files may (?) be too large and take a long time to load. Keeping an active transaction is a bad idea.
     * If the download to the storage failed, we will manually delete the entry
     *
     * @throws [StorageException.InteractingFailed]
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun store(
        originalName: String,
        payload: ByteArray,
        metadata: AttachmentMetadata,
        to: AttachmentCollection
    ): AttachmentId {
        val id = AttachmentId.next()

        store.add(id, originalName, metadata)

        try {
            filesystem.put(payload, FileData(id.toFileName(), Folder(to.value)))
        } catch (e: Throwable) {
            /** If the file could not be saved, delete the link to it*/
            store.remove(id)

            throw e
        }

        return id
    }

    /**
     * Receive attachment
     *
     * @throws [StorageException.InteractingFailed]
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun receive(id: AttachmentId, from: AttachmentCollection): Attachment? {
        val attachment = store.find(id) ?: return null

        return Attachment(
            name = attachment.name,
            metadata = attachment.attachmentMetadata,
            payload = filesystem.read(FileData(id.toFileName(), from.toFolder()))
        )
    }

    /**
     * Remove stored attachment
     *
     * @throws [StorageException.InteractingFailed]
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun remove(id: AttachmentId, from: AttachmentCollection) = try {
        filesystem.remove(FileData(id.toFileName(), from.toFolder()))
    } finally {
        store.remove(id)
    }
}

private fun AttachmentId.toFileName(): FileName = FileName(uuid.toString())
private fun AttachmentCollection.toFolder(): Folder = Folder(value)
