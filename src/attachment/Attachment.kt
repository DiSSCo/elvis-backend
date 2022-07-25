package org.synthesis.attachment

import com.fasterxml.jackson.annotation.JsonValue
import java.nio.ByteBuffer
import java.util.*

data class Attachment(
    val name: String,
    val metadata: AttachmentMetadata,
    val payload: ByteBuffer
)

data class AttachmentId(
    @JsonValue
    val uuid: UUID
) {
    companion object Factory {
        fun next(): AttachmentId = AttachmentId(UUID.randomUUID())
    }
}

data class AttachmentMimeType(
    val base: String,
    val subType: String
)

data class AttachmentMetadata(
    val extension: String,
    val mimeType: AttachmentMimeType
)

data class AttachmentCollection(
    val value: String
)

fun Attachment.fileNameWithExtension() = "$name.${metadata.extension}"
