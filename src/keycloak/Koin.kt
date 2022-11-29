package org.synthesis.keycloak

import org.koin.dsl.module
import org.slf4j.LoggerFactory
import org.synthesis.keycloak.admin.DefaultKeycloakAdminApiClient
import org.synthesis.keycloak.admin.KeycloakAdminApiClient
import org.synthesis.keycloak.api.DefaultKeycloakClient
import org.synthesis.keycloak.api.KeycloakClient

val keycloakModule = module {
    single {
        KeycloakRealm(
            getProperty("KEYCLOAK_REALM")
        )
    }

    single {
        KeycloakConfiguration(
            activeRealm = get(),
            serverUrl = getProperty("KEYCLOAK_SERVER_URL"),
            uiClientInfo = KeycloakApiClient(
                id = getProperty("KEYCLOAK_UI_CLIENT_ID")
            )
        )
    }

    single<KeycloakAdminApiClient> {
        DefaultKeycloakAdminApiClient(
            configuration = get(),
            logger = LoggerFactory.getLogger("keycloak")
        )
    }

    single<KeycloakClient> {
        DefaultKeycloakClient(
            configuration = get(),
            client = get(),
            logger = LoggerFactory.getLogger("keycloak")
        )
    }
}
