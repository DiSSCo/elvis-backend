package org.synthesis.calls.request

import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDateTime
import java.util.*
import org.synthesis.account.UserAccountId
import org.synthesis.calls.CallId
import org.synthesis.calls.CallType
import org.synthesis.country.Country

data class CallRequestId(
    @JsonValue
    val uuid: UUID
) {
    companion object {
        fun next(): CallRequestId = CallRequestId(UUID.randomUUID())
    }

    override fun toString(): String = uuid.toString()
}

/**
 * Call request interface marker
 */
interface CallRequest {
    fun id(): CallRequestId

    fun callId(): CallId

    fun callType(): CallType

    fun title(): String?

    fun description(): String?

    fun createdAt(): LocalDateTime

    fun deletedAt(): LocalDateTime?

    fun authorId(): UserAccountId

    fun country(): Country?
}
