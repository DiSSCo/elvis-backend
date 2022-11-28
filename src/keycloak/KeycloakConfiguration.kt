package org.synthesis.keycloak

data class KeycloakConfiguration(
    val activeRealm: KeycloakRealm,
    val serverUrl: String,
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

data class KeycloakApiClient(
    val id: String,
)
