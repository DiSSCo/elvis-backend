package org.synthesis.auth

import org.koin.dsl.module
import org.synthesis.auth.encrypt.BCryptHasher
import org.synthesis.auth.encrypt.PasswordHasher

val authModule = module {

    single<PasswordHasher> {
        BCryptHasher()
    }

    single<AuthorizationService> {
        KeycloakAuthorizationService(
            keycloakClient = get()
        )
    }
}
