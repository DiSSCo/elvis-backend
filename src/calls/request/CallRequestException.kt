package org.synthesis.calls.request

import java.util.*
import org.synthesis.account.UserAccountId
import org.synthesis.calls.request.ta.TaCallRequest
import org.synthesis.calls.request.ta.scoring.ScoreFormId
import org.synthesis.calls.request.va.VaCallRequest
import org.synthesis.country.CountryCode
import org.synthesis.institution.InstitutionId

sealed class CallRequestException(message: String) : Exception(message) {
    class RequestNotApproved : CallRequestException("Request not approved")

    class InstitutionAlreadyAdded(id: InstitutionId) : CallRequestException("Institution `$id` already added")

    class InstitutionNotAdded(id: InstitutionId) : CallRequestException("Institution `$id` not added")

    class UnableToSendRequest(id: CallRequestId) :
        CallRequestException("Request (`${id.uuid}`) can only be sent in Draft status")

    class NoInstitutionAdded(id: CallRequestId) :
        CallRequestException("Unable to submit request `${id.uuid}`: no institute has been added")

    class AttemptToCloseUnapprovedRequest(id: CallRequestId) :
        CallRequestException("Attempt to close an unapproved request `${id.uuid}`")

    class AttemptToApproveNotSubmittedRequest(status: VaCallRequest.Status) :
        CallRequestException("Cant approve request with status `$status`")

    class AttemptToApproveNotSubmittedRequestTa(status: TaCallRequest.Status) :
        CallRequestException("Cant approve request with status `$status`")

    class CantFindAssociatedInstitute(id: UserAccountId) :
        CallRequestException("Cant find associated institute for coordinator `${id.uuid}`")

    class UnableToDeleteRequest(id: CallRequestId) :
        CallRequestException("Request (`${id.uuid}`) can only be deleted in Draft status")

    class CantFindAssociatedCoordinator(id: InstitutionId) :
        CallRequestException("Cant find associated coordinator for institute `${id.grid.value}`")

    class IncorrectInstitutionCountry(message: String) : CallRequestException(message)

    class UserCantScoreRequest(id: UserAccountId, countryCode: CountryCode) :
        CallRequestException(
            "The `${id.uuid}` user is not allowed to evaluate the request as it does not belong to the `${countryCode.id}` country"
        )

    class ScoreFormNotAdded(id: ScoreFormId) : CallRequestException("Institution `${id.id}` not added")
}
