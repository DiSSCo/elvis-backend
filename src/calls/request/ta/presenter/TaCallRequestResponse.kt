package org.synthesis.calls.request.ta.presenter

import java.util.*
import org.synthesis.account.OrcId
import org.synthesis.account.UserAccount
import org.synthesis.account.UserAccountStatus

data class TaCallRequestResponse(
    val id: UUID,
    val callId: UUID,
    val status: String,
    val fieldValues: List<FieldValue>,
    val institutions: List<Institution>,
    val scoreFormId: List<Scoring>,
    val creatorData: TaCallRequesterData?
) {
    data class FieldValue(
        val fieldId: String,
        val value: org.synthesis.formbuilder.FieldValue?
    )

    data class Institution(
        val id: String,
        val name: String,
        val status: String,
        val fieldValues: List<FieldValue>
    )
    data class Scoring(
        val id: UUID
    )
}

data class TaCallRequesterData(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val orcId: OrcId?,
    val status: String = "Enabled"
)

fun UserAccount.asTaCallRequesterData() = TaCallRequesterData(
    id = id.uuid,
    email = email,
    firstName = fullName.firstName,
    lastName = fullName.lastName,
    orcId = attributes.orcId,
    status = when (status) {
        is UserAccountStatus.Banned -> "Disabled"
        is UserAccountStatus.Inactive -> "Disabled"
        is UserAccountStatus.Active -> "Enabled"
    }
)
