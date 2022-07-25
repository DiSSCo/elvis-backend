package org.synthesis.calls.request.ta.scoring

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

data class ScoreFormId(
    @JsonValue
    val id: UUID
)
