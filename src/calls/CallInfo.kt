package org.synthesis.calls

import java.time.LocalDateTime

data class CallInfo(
    var name: String,
    var description: String,
    var additionalInformation: String,
    var scoring: ScoringData?
)
data class ScoringData(
    var endDate: LocalDateTime?,
    var weight: ScoringWeight?
)
data class ScoringWeight(
    val methodology: String?,
    val researchExcellence: String?,
    val supportStatement: String?,
    val justification: String?,
    val gainsOutputs: String?,
    val merit: String?,
    val societalChallenge: String?,
)
