package org.synthesis.calls.request.ta.scoring

import java.time.LocalDateTime
import org.synthesis.account.UserAccountId
import org.synthesis.calls.request.CallRequestId
import org.synthesis.formbuilder.DynamicForm

data class ScoreResponse(
    val id: ScoreFormId,
    val requestId: CallRequestId,
    val form: DynamicForm,
    val scorerId: UserAccountId,
    val deletedAt: LocalDateTime?
)
