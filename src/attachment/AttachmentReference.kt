package org.synthesis.attachment

import java.time.LocalDateTime

data class AttachmentReference(
    val id: AttachmentId,
    val name: String,
    val attachmentMetadata: AttachmentMetadata,
    val addedAt: LocalDateTime
)
