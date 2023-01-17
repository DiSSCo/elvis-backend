package org.synthesis.environment

import kotlinx.coroutines.runBlocking
import org.synthesis.account.*
import org.synthesis.account.registration.RegistrationHandler
import org.synthesis.account.registration.UserAccountRegistrationCredentials
import org.synthesis.account.registration.UserAccountRegistrationData
import org.synthesis.country.CountryCode
import org.synthesis.institution.InstitutionId

class DefaultUserImporter(
    val registrationHandler: RegistrationHandler
) {
    private val institutionId = InstitutionId.fromString("grid.35937.3b")
    private val users: List<UserAccountRegistrationData> = listOf(
        UserAccountRegistrationData(
            email = "requester@picturae.com",
            groups = listOf("requester"),
            fullName = UserFullName(
                firstName = "Requester",
                lastName = "Picturae"
            ),
            attributes = UserAccountAttributes(
                orcId = OrcId("0000-0000-0000-0000"),
                institutionId = null,
                relatedInstitutionId = institutionId,
                gender = Gender.MALE,
                birthDate = null,
                nationality = "Belgium",
                countryOtherInstitution = null
            ),
            credentials = UserAccountRegistrationCredentials.ClearPassword("Test12345")
        ),
        UserAccountRegistrationData(
            email = "admin@picturae.com",
            groups = listOf("administrator"),
            fullName = UserFullName(
                firstName = "Administrator",
                lastName = "Picturae"
            ),
            attributes = UserAccountAttributes(
                orcId = null,
                institutionId = null,
                relatedInstitutionId = null,
                gender = Gender.MALE,
                birthDate = null,
                nationality = "Netherlands",
                countryOtherInstitution = "Netherlands"
            ),
            credentials = UserAccountRegistrationCredentials.ClearPassword("Test12345")
        ),
        UserAccountRegistrationData(
            email = "moderator@picturae.com",
            groups = listOf("institution moderator"),
            fullName = UserFullName(
                firstName = "Institution moderator",
                lastName = "Picturae"
            ),
            attributes = UserAccountAttributes(
                orcId = null,
                institutionId = institutionId,
                relatedInstitutionId = institutionId,
                gender = Gender.MALE,
                birthDate = null,
                nationality = null,
                countryOtherInstitution = null
            ),
            credentials = UserAccountRegistrationCredentials.ClearPassword("Test12345")
        ),
        UserAccountRegistrationData(
            email = "va_coordinator@picturae.com",
            groups = listOf("va coordinator"),
            fullName = UserFullName(
                firstName = "VA Coordinator",
                lastName = "Picturae"
            ),
            attributes = UserAccountAttributes(
                orcId = null,
                institutionId = institutionId,
                relatedInstitutionId = institutionId,
                gender = Gender.MALE,
                birthDate = null,
                nationality = null,
                countryOtherInstitution = null
            ),
            credentials = UserAccountRegistrationCredentials.ClearPassword("Test12345")
        ),
        UserAccountRegistrationData(
            email = "ta_coordinator@picturae.com",
            groups = listOf("ta coordinator"),
            fullName = UserFullName(
                firstName = "TA Coordinator",
                lastName = "Picturae"
            ),
            attributes = UserAccountAttributes(
                orcId = null,
                institutionId = institutionId,
                relatedInstitutionId = institutionId,
                gender = Gender.MALE,
                birthDate = null,
                nationality = null,
                countryOtherInstitution = null
            ),
            credentials = UserAccountRegistrationCredentials.ClearPassword("Test12345")
        ),
        UserAccountRegistrationData(
            email = "ta_scorer@picturae.com",
            groups = listOf("ta scorer"),
            fullName = UserFullName(
                firstName = "TA Scorer",
                lastName = "Picturae"
            ),
            attributes = UserAccountAttributes(
                orcId = null,
                institutionId = null,
                relatedInstitutionId = null,
                gender = Gender.MALE,
                birthDate = null,
                countryCode = CountryCode("GB"),
                nationality = null,
                countryOtherInstitution = null
            ),
            credentials = UserAccountRegistrationCredentials.ClearPassword("Test12345")
        ),
        UserAccountRegistrationData(
            email = "taf_admin@picturae.com",
            groups = listOf("taf admin"),
            fullName = UserFullName(
                firstName = "TAF Admin",
                lastName = "Picturae"
            ),
            attributes = UserAccountAttributes(
                orcId = null,
                institutionId = null,
                relatedInstitutionId = null,
                gender = Gender.MALE,
                birthDate = null,
                countryCode = CountryCode("GB"),
                nationality = null,
                countryOtherInstitution = null
            ),
            credentials = UserAccountRegistrationCredentials.ClearPassword("Test12345")
        )
    )

    fun execute() = runBlocking {
        users.forEach {
            try {
                registrationHandler.handle(
                    data = it,
                    withNotification = false
                )
            } catch (e: UserAccountException.AlreadyRegistered) {
                /** Not interests */
            }
        }
    }
}
