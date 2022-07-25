package org.synthesis.calls.request.va.attachmnent

import java.time.LocalDateTime
import org.synthesis.attachment.AttachmentId
import org.synthesis.attachment.AttachmentMetadata
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.attachment.CallRequestAttachmentId
import org.synthesis.calls.request.attachment.CallRequestAttachmentOwner
import org.synthesis.institution.InstitutionId

data class VaCallRequestAttachment(
    val id: CallRequestAttachmentId,
    val attachmentId: AttachmentId,
    val callRequestId: CallRequestId,
    val institutionId: InstitutionId?,
    val owner: CallRequestAttachmentOwner,
    val name: String,
    val metadata: AttachmentMetadata,
    val addedAt: LocalDateTime
)
