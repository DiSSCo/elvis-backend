package org.synthesis.auth

import org.synthesis.keycloak.api.Permission

sealed class AuthException(message: String) : Exception(message) {
    class NoPublicKeyFound : AuthException("No public key found")
    class IncorrectAuthToken : AuthException("Received incorrect auth token")
    class NotAllowed(vararg permissions: Permission) :
        AuthException(
            "The client doesn\'t have access to the following resources: " +
                    permissions.joinToString(", ") { it.resource }
        )
}
