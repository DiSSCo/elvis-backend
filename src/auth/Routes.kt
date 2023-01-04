package org.synthesis.auth

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject
import org.synthesis.keycloak.KeycloakConfiguration

fun Route.authRoutes() {
    val keycloakConfiguration by inject<KeycloakConfiguration>()

    get("/config") {
        call.respond(
            mapOf(
                "authConfig" to AuthConfig(
                    realm = keycloakConfiguration.activeRealm.value,
                    url = keycloakConfiguration.serverUrl,
                    clientId = keycloakConfiguration.uiClientInfo.id
                ),
                "available_groups" to listOf(
                    "administrator",
                    "institution moderator",
                    "requester",
                    "ta coordinator",
                    "ta scorer",
                    "taf admin",
                    "va coordinator"
                )
            )
        )
    }
}
