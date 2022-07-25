package org.synthesis.keycloak.admin

sealed class KeycloakUserCredentials {
    abstract val temporary: Boolean

    data class ClearPassword(
        val password: String,
        override val temporary: Boolean
    ) : KeycloakUserCredentials()

    data class HashedPassword(
        val hash: String,
        val algorithm: String = "bcrypt",
        val iterations: Int = 10,
        override val temporary: Boolean
    ) : KeycloakUserCredentials()
}
