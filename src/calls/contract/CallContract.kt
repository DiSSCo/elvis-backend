package org.synthesis.calls.contract

import java.time.LocalDateTime
import java.util.*
import org.synthesis.calls.CallId
import org.synthesis.calls.ScoringData
import org.synthesis.calls.ScoringWeight

sealed class CallCommand {
    data class Create(
        val type: String,
        val name: String,
        val description: String,
        val additionalInfo: String,
        val startDate: LocalDateTime,
        val endDate: LocalDateTime?,
        val scoringEndDate: LocalDateTime?,
        val scoringWeight: ScoringWeight?
    ) : CallCommand()

    data class Update(
        val name: String,
        val description: String,
        val additionalInfo: String,
        val startDate: LocalDateTime,
        val endDate: LocalDateTime?,
        val scoringEndDate: LocalDateTime?,
        val scoringWeight: ScoringWeight?
    ) : CallCommand()

    data class Delete(
        val id: CallId
    ) : CallCommand()
}

data class CallResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val additionalInfo: String,
    val type: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime?,
    val status: String,
    val scoring: ScoringData?
)
