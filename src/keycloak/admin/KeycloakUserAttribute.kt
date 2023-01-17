package org.synthesis.keycloak.admin

import java.time.LocalDate
import org.keycloak.representations.idm.UserRepresentation
import org.synthesis.account.Gender
import org.synthesis.account.OrcId
import org.synthesis.account.UserAccountAttributes
import org.synthesis.country.CountryCode
import org.synthesis.institution.InstitutionId

/**
 * Converting the internal representation of attributes to the Keycloak side representation.
 */
fun UserAccountAttributes.toKeycloakRepresentation() = mapOf(
    KeycloakAttribute.orcIdAttributeName to listOf(orcId?.id),
    KeycloakAttribute.institutionAttributeName to listOf(institutionId?.grid?.value),
    KeycloakAttribute.relatedInstitutionAttributeName to listOf(relatedInstitutionId?.grid?.value),
    KeycloakAttribute.genderAttributeName to listOf(gender.name.lowercase()),
    KeycloakAttribute.birthDateAttributeName to listOf(birthDate?.toString()),
    KeycloakAttribute.countryCodeAttributeName to listOf(countryCode?.id),
    KeycloakAttribute.nationalityAttributeName to listOf(nationality),
    KeycloakAttribute.homeInstitutionIdAttributeName to listOf(homeInstitutionId)
)

/**
 * Retrieving user attributes.
 */
fun UserRepresentation.attributes() = UserAccountAttributes(
    orcId = orcId(),
    institutionId = institutionId(),
    relatedInstitutionId = relatedInstitutionId(),
    gender = gender(),
    birthDate = birthDate(),
    countryCode = countryCode(),
    nationality = nationality(),
    homeInstitutionId = homeInstitutionId(),
    countryOtherInstitution = countryOtherInstitution()
)

/**
 * Getting user gender.
 */
fun UserRepresentation.gender(): Gender =
    (attributes ?: mapOf())[KeycloakAttribute.genderAttributeName]
        ?.firstOrNull()
        ?.let { Gender.valueOf(it.uppercase()) }
        ?: Gender.OTHER

/**
 * Getting the identifier of the institution to which the user is linked (One of the possible attributes).
 */
fun UserRepresentation.institutionId(): InstitutionId? =
    (attributes ?: mapOf())[KeycloakAttribute.institutionAttributeName]
        ?.firstOrNull()
        ?.let { InstitutionId.fromString(it) }

/**
 * Get related institution id.
 */
fun UserRepresentation.relatedInstitutionId(): InstitutionId? =
    (attributes ?: mapOf())[KeycloakAttribute.relatedInstitutionAttributeName]
        ?.firstOrNull()
        ?.let { InstitutionId.fromString(it) }

/**
 * Getting the user's OrcID (One of the possible attributes).
 */
fun UserRepresentation.orcId(): OrcId? =
    (attributes ?: mapOf())[KeycloakAttribute.orcIdAttributeName]
        ?.firstOrNull()
        ?.let { OrcId(it) }

/**
 * Getting the user's birth date (One of the possible attributes).
 */
fun UserRepresentation.birthDate(): LocalDate? =
    (attributes ?: mapOf())[KeycloakAttribute.birthDateAttributeName]
        ?.firstOrNull()
        ?.let { LocalDate.parse(it) }

/**
 * Getting the user's country code(One of the possible attributes).
 */
fun UserRepresentation.countryCode(): CountryCode? =
    (attributes ?: mapOf())[KeycloakAttribute.countryCodeAttributeName]
        ?.firstOrNull()
        ?.let { CountryCode(it) }

/**
 * Getting the user's nationality(One of the possible attributes).
 */
fun UserRepresentation.nationality(): String? =
    (attributes ?: mapOf())[KeycloakAttribute.nationalityAttributeName]
        ?.firstOrNull()

/**
 * Getting the user's potential other country(One of the possible attributes).
 */
fun UserRepresentation.countryOtherInstitution(): String? =
    (attributes ?: mapOf())[KeycloakAttribute.countryOtherInstitutionAttributeName]
        ?.firstOrNull()

/**
 * Getting the user's home institution(One of the possible attributes).
 */
fun UserRepresentation.homeInstitutionId(): String? =
    (attributes ?: mapOf())[KeycloakAttribute.homeInstitutionIdAttributeName]
        ?.firstOrNull()

/**
 * The name of the attributes on the Keycloak side
 */
object KeycloakAttribute {
    const val orcIdAttributeName = "orcId"
    const val institutionAttributeName = "institutionId"
    const val relatedInstitutionAttributeName = "relatedInstitutionId"
    const val genderAttributeName = "gender"
    const val birthDateAttributeName = "birthDate"
    const val countryCodeAttributeName = "countryCode"
    const val nationalityAttributeName = "nationality"
    const val homeInstitutionIdAttributeName = "homeInstitutionId"
    const val countryOtherInstitutionAttributeName = "countryOtherInstitution"
}
