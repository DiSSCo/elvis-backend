package org.synthesis.auth.ktor

import io.ktor.server.auth.*
import org.synthesis.account.UserAccountFinder
import org.synthesis.account.manage.store.AccountStore
import org.synthesis.auth.AuthService
import org.synthesis.keycloak.KeycloakConfiguration

class KtorAuthConfigurer(
    private val authService: AuthService,
    private val config: KeycloakConfiguration,
    private val accountFinder: UserAccountFinder,
    private val accountStore: AccountStore
) {
    fun Authentication.Configuration.configure() {
        keycloak {
            verifier = { accessToken ->
                authService.verifyToken(accessToken)
            }
            validate { jwtCredential ->
                val principal = jwtCredential.principal(config.activeRealm, config.uiClientInfo)

                accountStore.syncRoles(principal.id, principal.roles)

                val userAccount = accountFinder.find(principal.id)

                userAccount
            }
        }
    }
}
