package org.synthesis.calls.request.attachment

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

data class CallRequestAttachmentId(
    @JsonValue
    val uuid: UUID
) {
    companion object {
        fun next(): CallRequestAttachmentId = CallRequestAttachmentId(UUID.randomUUID())
    }
}
