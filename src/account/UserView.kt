package org.synthesis.account

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class UserView(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val birthDateTime: LocalDate?,
    val gender: String,
    val groups: List<String>,
    val attributes: Map<String, String?>,
    val bannedAt: LocalDateTime?,
    val bannedWithReason: String?
)

fun UserAccount.asView(): UserView {
    var bannedAt: LocalDateTime? = null
    var bannedWithReason: String? = null

    if (status is UserAccountStatus.Banned) {
        bannedAt = status.dateTime
        bannedWithReason = status.reason
    }

    return UserView(
        id = id.uuid,
        email = email,
        firstName = fullName.firstName,
        lastName = fullName.lastName,
        birthDateTime = attributes.birthDate,
        gender = attributes.gender.name.lowercase(),
        groups = groups,
        attributes = mapOf(
            "orcId" to attributes.orcId?.id,
            "institutionId" to attributes.institutionId?.grid?.value,
            "relatedInstitutionId" to attributes.relatedInstitutionId?.grid?.value,
            "homeInstitutionId" to attributes.homeInstitutionId,
            "nationality" to attributes.nationality,
            "countryCode" to attributes.countryCode?.id?.uppercase(),
            "countryOtherInstitution" to attributes.countryOtherInstitution
        ),
        bannedAt = bannedAt,
        bannedWithReason = bannedWithReason
    )
}
