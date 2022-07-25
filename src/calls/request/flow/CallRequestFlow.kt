package org.synthesis.calls.request.flow

import org.synthesis.account.UserAccount
import org.synthesis.calls.Call
import org.synthesis.calls.CallId
import org.synthesis.calls.ScoreFields
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.ta.scoring.ScoreFormId
import org.synthesis.formbuilder.FieldId
import org.synthesis.formbuilder.FieldValue
import org.synthesis.formbuilder.GroupId
import org.synthesis.institution.InstitutionId

/**
 * Call request flow marker
 */
interface CallRequestFlow {
    suspend fun handle(call: Call, command: CallRequestCommand): Any?
}

sealed class CallRequestCommand {
    abstract val callId: CallId?
    abstract val callRequestId: CallRequestId?
    abstract val user: UserAccount?

    data class CreateRequest(
        override val callId: CallId,
        override val user: UserAccount,
        override val callRequestId: CallRequestId
    ) : CallRequestCommand()

    data class SetRequestFieldValue(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
        val institutionId: InstitutionId?,
        val fieldId: FieldId,
        val fieldValue: FieldValue?
    ) : CallRequestCommand()

    data class DeleteFieldGroup(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
        val institutionId: InstitutionId?,
        val fieldGroupId: GroupId,
    ) : CallRequestCommand()

    data class AddInstitutionForm(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
        val institutionId: InstitutionId
    ) : CallRequestCommand()

    data class DeleteInstitutionForm(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
        val institutionId: InstitutionId
    ) : CallRequestCommand()

    data class SubmitRequest(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
    ) : CallRequestCommand()

    data class DeleteRequest(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
    ) : CallRequestCommand()

    data class CloseRequest(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
    ) : CallRequestCommand()

    data class ApproveRequest(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
    ) : CallRequestCommand()

    data class UndoRequestApprove(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
    ) : CallRequestCommand()

    data class WithdrawRequest(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
    ) : CallRequestCommand()

    data class Score(
        override val callId: CallId? = null,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
        val scoreForm: List<ScoreFields>,
        val scoreFormId: ScoreFormId
    ) : CallRequestCommand()

    data class CreateScore(
        override val callId: CallId?,
        override val user: UserAccount,
        override val callRequestId: CallRequestId
    ) : CallRequestCommand()

    data class DeleteScore(
        override val callId: CallId?,
        override val user: UserAccount,
        override val callRequestId: CallRequestId,
        val scoreFormId: ScoreFormId
    ) : CallRequestCommand()

    data class FindRequest(
        override val callId: CallId,
        override val user: UserAccount,
        override val callRequestId: CallRequestId
    ) : CallRequestCommand()

    data class ExportRequest(
        override val callId: CallId? = null,
        override val user: UserAccount? = null,
        override val callRequestId: CallRequestId
    ) : CallRequestCommand()
}
