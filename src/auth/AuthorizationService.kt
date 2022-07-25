package org.synthesis.auth

import org.synthesis.keycloak.api.KeycloakClient
import org.synthesis.keycloak.api.Permission

interface AuthorizationService {

    /**
     * Checks the ability for the specified token to perform an operation.
     */
    suspend fun isGranted(token: String, vararg permissions: Permission): Boolean
}

class KeycloakAuthorizationService(
    private val keycloakClient: KeycloakClient
) : AuthorizationService {

    override suspend fun isGranted(token: String, vararg permissions: Permission): Boolean =
        keycloakClient.allowed(token, *permissions)
}
