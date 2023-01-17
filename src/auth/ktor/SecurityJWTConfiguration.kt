package org.synthesis.auth.ktor

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.interfaces.Payload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import org.koin.ktor.ext.inject
import org.synthesis.account.UserAccountFinder
import org.synthesis.account.manage.store.AccountStore
import org.synthesis.keycloak.KeycloakConfiguration
import org.synthesis.keycloak.KeycloakConfiguration.Companion.certificateUrl
import org.synthesis.keycloak.KeycloakConfiguration.Companion.realmUrl
import java.net.URL
import java.util.concurrent.TimeUnit


fun Application.configureSecurity() {

    val accountStore by inject<AccountStore>()
    val accountFinder by inject<UserAccountFinder>()
    val config by inject<KeycloakConfiguration>()

    val jwkProvider = JwkProviderBuilder(
        URL(config.certificateUrl())
    )
        .cached(10, 24, TimeUnit.HOURS)
        .build()

    install(Authentication) {
        jwt {
            realm = config.activeRealm.value
            verifier(jwkProvider, config.realmUrl()) {
                acceptLeeway(3)
            }
            validate { credentials ->
                val principal = JWTCredential(credentials.payload).principal(
                    config.activeRealm,
                    config.uiClientInfo
                )

                accountStore.syncRoles(principal.id, principal.roles)

                val userAccount = accountFinder.find(principal.id)

                userAccount
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}

/**
 * Creating a class containing authentication parameters.
 */
class JWTCredential(val payload: Payload) : Credential

