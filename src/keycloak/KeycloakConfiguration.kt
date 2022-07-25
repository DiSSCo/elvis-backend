package org.synthesis.keycloak

data class KeycloakConfiguration(
    val activeRealm: KeycloakRealm,
    val serverUrl: String,
    val credentials: KeycloakCredentials,
    val apiClientInfo: KeycloakApiClient,
    val adminClientInfo: KeycloakApiClient,
    val uiClientInfo: KeycloakApiClient
) {
    companion object {
        fun KeycloakConfiguration.realmUrl() = "$serverUrl/realms/${activeRealm.value}"
        fun KeycloakConfiguration.certificateUrl() = "${realmUrl()}/protocol/openid-connect/certs"
        fun KeycloakConfiguration.tokenUrl() = "${realmUrl()}/protocol/openid-connect/token"
        fun KeycloakConfiguration.resourcesUrl() = "${realmUrl()}/authz/protection/resource_set"
    }
}

data class KeycloakRealm(
    val value: String
)

data class KeycloakCredentials(
    val username: String,
    val password: String
)

data class KeycloakApiClient(
    val id: String,
    val secret: String?
)
