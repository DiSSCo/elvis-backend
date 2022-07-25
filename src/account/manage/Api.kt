package org.synthesis.account.manage

import org.synthesis.account.UserFullName

sealed class UserManageCommand {

    data class Create(
        val email: String,
        val fullName: UserFullName,
        val attributes: Attributes,
        val password: String,
        val groups: List<String>,
        val sendEmail: Boolean
    ) : UserManageCommand()

    data class Ban(
        val reason: String
    ) : UserManageCommand()

    data class Promote(
        val group: String
    ) : UserManageCommand()

    data class Demote(
        val group: String
    ) : UserManageCommand()
}

data class Attributes(
    val orcId: String?,
    val institutionId: String?,
    val gender: String?,
    val relatedInstitutionId: String?,
    val birthDate: String?,
    val nationality: String?,
    val countryOtherInstitution: String?
)
