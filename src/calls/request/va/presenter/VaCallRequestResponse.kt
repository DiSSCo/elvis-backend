package org.synthesis.calls.request.va.presenter

import java.util.*
import org.synthesis.account.OrcId
import org.synthesis.account.UserAccount
import org.synthesis.account.UserAccountStatus

data class VaCallRequestResponse(
    val id: UUID,
    val callId: UUID,
    val status: String,
    val fieldValues: List<FieldValue>,
    val institutions: List<Institution>,
    val creatorData: VaCallRequesterData?
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
}

data class VaCallRequesterData(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val orcId: OrcId?,
    val status: String = "Enabled"
)

fun UserAccount.asVaCallRequesterData() = VaCallRequesterData(
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
