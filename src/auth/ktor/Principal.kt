package org.synthesis.auth.ktor

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.LinkedHashMap
import org.synthesis.account.*
import org.synthesis.institution.InstitutionId
import org.synthesis.keycloak.KeycloakApiClient
import org.synthesis.keycloak.KeycloakRealm
import org.synthesis.keycloak.admin.KeycloakAttribute
import org.synthesis.keycloak.admin.KeycloakUserGroup

/**
 * Parsing a token and creating an internal user representation based on it.
 */
internal fun JWTCredential.principal(realm: KeycloakRealm, client: KeycloakApiClient): UserAccount = UserAccount(
    id = id(),
    email = payload.getClaim("email").asString(),
    groups = groups(),
    roles = roles(client),
    fullName = UserFullName(
        firstName = payload.getClaim("given_name").asString(),
        lastName = payload.getClaim("family_name").asString()
    ),
    realmId = realm.value,
    attributes = attributes(),
    synchronizedAt = LocalDateTime.now(),
    status = UserAccountStatus.Active()
)

/**
 * Parsing a token and retrieving all roles that are assigned to a user
 */
@Suppress("UNCHECKED_CAST")
private fun JWTCredential.roles(client: KeycloakApiClient): List<String> {
    @Suppress("UnsafeCast")
    val permissionsMap = payload
        .getClaim("resource_access")
        .asMap()[client.id] as LinkedHashMap<String, List<String>>

    return permissionsMap["roles"]?.toList() ?: listOf()
}

/**
 * Parsing a token and retrieving all groups the user is a member of.
 */
private fun JWTCredential.groups(): List<String> {
    val groups = payload
        .getClaim("groups")
        .asList(KeycloakUserGroup::class.java)

    return groups?.map { it.toApplicationRepresentation() } ?: listOf()
}

/**
 * Parsing a token and retrieving all user attributes
 */
private fun JWTCredential.attributes(): UserAccountAttributes = UserAccountAttributes(
    orcId = orcIdAttribute(),
    institutionId = institutionIdAttribute(),
    relatedInstitutionId = relatedInstitutionIdAttribute(),
    gender = genderAttribute(),
    birthDate = birthDateAttribute(),
    nationality = nationalityAttribute(),
    countryOtherInstitution = countryOtherInstitutionAttribute()
)

/**
 * Parsing the token and extracting the user ID
 */
private fun JWTCredential.id(): UserAccountId = UserAccountId(UUID.fromString(payload.getClaim("sub").asString()))

/**
 * Parsing the token and extracting the OrcId
 */
private fun JWTCredential.orcIdAttribute(): OrcId? = payload
    .getClaim(KeycloakAttribute.orcIdAttributeName)
    ?.asString()
    ?.let { OrcId(it) }

/**
 * Parsing the token and extracting the birthdate
 */
private fun JWTCredential.birthDateAttribute(): LocalDate? = payload
    .getClaim(KeycloakAttribute.birthDateAttributeName)
    ?.asString()
    ?.let { LocalDate.parse(it) }

/**
 * Parsing the token and extracting the OrcId
 */
private fun JWTCredential.genderAttribute(): Gender = payload
    .getClaim(KeycloakAttribute.genderAttributeName)
    ?.asString()
    ?.let { Gender.valueOf(it.uppercase()) }
    ?: Gender.OTHER

/**
 * Parsing the token and extracting the identifier of the institution to which the user is bound.
 */
private fun JWTCredential.institutionIdAttribute(): InstitutionId? =
    payload
        .getClaim(KeycloakAttribute.institutionAttributeName)
        ?.asString()
        ?.let { InstitutionId.fromString(it) }

/**
 * Parsing the token and extracting the related institution id.
 */
private fun JWTCredential.relatedInstitutionIdAttribute(): InstitutionId? =
    payload
        .getClaim(KeycloakAttribute.relatedInstitutionAttributeName)
        ?.asString()
        ?.let { InstitutionId.fromString(it) }

/**
 * Parsing the token and extracting the identifier of the institution to which the user is bound.
 */
private fun JWTCredential.nationalityAttribute(): String? =
    payload
        .getClaim(KeycloakAttribute.nationalityAttributeName)
        ?.asString()

/**
 * Parsing the token and extracting the identifier of the institution to which the user is bound.
 */
private fun JWTCredential.countryOtherInstitutionAttribute(): String? =
    payload
        .getClaim(KeycloakAttribute.countryOtherInstitutionAttributeName)
        ?.asString()
