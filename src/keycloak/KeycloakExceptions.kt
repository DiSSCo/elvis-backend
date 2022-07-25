package org.synthesis.keycloak

import org.synthesis.account.UserAccountId

sealed class KeycloakExceptions(message: String, cause: Exception? = null) : Exception(message, cause) {

    class UserAlreadyExists(realm: KeycloakRealm, email: String) :
        KeycloakExceptions("User with email `$email` already registered in `${realm.value}` realm")

    class UserNotExists(realm: KeycloakRealm, id: UserAccountId) :
        KeycloakExceptions("User with id `${id.uuid}` not found in `${realm.value}` realm")

    class OperationFailed(realm: KeycloakRealm, error: Exception) : KeycloakExceptions(
        "Keycloak api operation failed for realm `${realm.value}`: ${error.message}", error
    )

    class EntityAlreadyExists(message: String, realm: KeycloakRealm) : KeycloakExceptions(
        "$message in realm `${realm.value}`"
    )
}
