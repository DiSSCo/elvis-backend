package org.synthesis.auth

import org.koin.dsl.module
import org.synthesis.auth.encrypt.BCryptHasher
import org.synthesis.auth.encrypt.PasswordHasher
import org.synthesis.auth.keycloak.KeycloakAuthService
import org.synthesis.auth.ktor.KtorAuthConfigurer

val authModule = module {

    single<PasswordHasher> {
        BCryptHasher()
    }

    single<AuthService> {
        KeycloakAuthService(
            config = get()
        )
    }

    single {
        KtorAuthConfigurer(
            authService = get(),
            config = get(),
            accountFinder = get(),
            accountStore = get()
        )
    }

    single<AuthorizationService> {
        KeycloakAuthorizationService(
            keycloakClient = get()
        )
    }
}
