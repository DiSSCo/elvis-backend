package org.synthesis.keycloak.admin

import java.util.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.synthesis.account.UserAccount
import org.synthesis.account.UserAccountId
import org.synthesis.account.UserAccountStatus
import org.synthesis.account.UserFullName
import org.synthesis.keycloak.KeycloakRealm

enum class KeycloakAccountStatus {
    ACTIVE,
    BANNED
}

/**
 * Converting the internal representation of customer credentials to Keycloak representation.
 */
internal fun KeycloakUserCredentials.toKeycloakRepresentation(): CredentialRepresentation {
    val credential = CredentialRepresentation()

    credential.type = CredentialRepresentation.PASSWORD
    credential.isTemporary = temporary

    when (this) {
        is KeycloakUserCredentials.ClearPassword -> credential.value = password
        is KeycloakUserCredentials.HashedPassword -> {
            credential.secretData = JsonObject(
                mapOf("value" to JsonPrimitive(hash))
            ).toString()

            credential.credentialData = "{\"hashIterations\":$iterations,\"algorithm\":\"${algorithm}\"}"
            credential.priority = 20
        }
    }

    return credential
}

/**
 * Converting the user representation into a Keycloak into the internal representation of the application
 */
internal fun UserRepresentation.applicationUser(realm: KeycloakRealm): UserAccount = UserAccount(
    id = UserAccountId(UUID.fromString(id)),
    email = email,
    fullName = UserFullName(
        firstName = firstName,
        lastName = lastName
    ),
    groups = groupCollection(),
    roles = roleCollection(realm),
    attributes = attributes(),
    realmId = realm.value,
    synchronizedAt = null,
    status = if (isEnabled) {
        UserAccountStatus.Active()
    } else {
        UserAccountStatus.Inactive()
    }
)

/**
 * Getting the groups a user is in.
 */
internal fun UserRepresentation.groupCollection(): List<String> = (groups?.filterNotNull() ?: listOf())
    .map { KeycloakUserGroup(it).toApplicationRepresentation() }

/**
 * Getting a list of roles that are assigned to a user
 */
internal fun UserRepresentation.roleCollection(forRealm: KeycloakRealm): List<String> =
    clientRoles?.get(forRealm.value)?.filterNotNull() ?: listOf()

/**
 * Convert internal user representation to Keycloak representation.
 */
internal fun UserAccount.userRepresentation(): UserRepresentation = UserRepresentation().apply {
    id = this@userRepresentation.id.uuid.toString()
    email = this@userRepresentation.email
    username = this@userRepresentation.email
    firstName = this@userRepresentation.fullName.firstName
    lastName = this@userRepresentation.fullName.lastName
    attributes = this@userRepresentation.attributes.toKeycloakRepresentation()
    groups = this@userRepresentation.groups
    isEnabled = this@userRepresentation.status is UserAccountStatus.Active
}
