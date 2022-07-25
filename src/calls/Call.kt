package org.synthesis.calls

import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDateTime
import java.util.*

enum class CallType {
    VA,
    TA
}

data class CallId(
    @JsonValue
    val uuid: UUID
) {
    companion object {
        fun next() = CallId(UUID.randomUUID())
    }
}

class Call(
    private val id: CallId,
    private var type: CallType,
    private var lifetime: CallLifeTime,
    private var info: CallInfo,
    private val createdAt: LocalDateTime = LocalDateTime.now(),
    private var deletedAt: LocalDateTime? = null
) {

    fun id(): CallId = id

    fun title(): String = info.name

    fun description(): String = info.description

    fun additionalInformation(): String = info.additionalInformation

    fun scoringEndDate(): LocalDateTime? = info.scoring?.endDate

    fun scoringWeight(): ScoringWeight? = info.scoring?.weight

    fun startDate(): LocalDateTime = lifetime.startDate

    fun endDate(): LocalDateTime? = lifetime.endDate

    fun type(): CallType = type

    fun createdAt(): LocalDateTime = createdAt

    /**
     * Indicates whether VA Call is open for incoming requests or not
     */
    fun opened(): Boolean = lifetime.isLive()

    fun deletedAt(): LocalDateTime? = deletedAt

    fun delete() {
        deletedAt = LocalDateTime.now()
    }
    fun addScoringEndDate(dateTime: LocalDateTime) {
        info.scoring?.endDate = dateTime
    }
    fun addScoringWeight(weight: ScoringWeight) {
        info.scoring?.weight = weight
    }

    /**
     * Change Call title
     */
    fun rename(name: String) {
        info.name = name
    }

    /**
     * Change Call description
     */
    fun updateDescription(description: String) {
        info.description = description
    }

    /**
     * Change Call lifetime
     */
    fun changeLifetime(startDate: LocalDateTime, endDate: LocalDateTime?) {
        lifetime = CallLifeTime.create(startDate, endDate)
    }

    /**
     * Update additional information data
     */
    fun updateAdditionalInfo(additionalInfo: String) {
        info.additionalInformation = additionalInfo
    }
}
