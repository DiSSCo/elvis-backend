package org.synthesis.auth.ktor

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.response.respond
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KeycloakAuthenticationProvider internal constructor(config: Config) : AuthenticationProvider(config) {
    internal val authHeader: (ApplicationCall) -> HttpAuthHeader? = config.authHeader
    internal val verifier = config.verifier
    internal val authenticationFunction = config.authenticationFunction
    internal val logger: Logger = LoggerFactory.getLogger("auth")

    class Config internal constructor() : AuthenticationProvider.Config(null) {
        internal var verifier: suspend ApplicationCall.(accessToken: String) -> Boolean = { false }
        internal val authHeader: (ApplicationCall) -> HttpAuthHeader? = { call ->
            call.request.parseAuthorizationHeader()
        }

        internal val challenge: JWTAuthChallengeFunction = { scheme, realm ->
            call.respond(
                UnauthorizedResponse(
                    HttpAuthHeader.Parameterized(
                        scheme,
                        mapOf(HttpAuthHeader.Parameters.Realm to realm)
                    )
                )
            )
        }

        internal var authenticationFunction: AuthenticationFunction<JWTCredential> = {
            throw NotImplementedError(
                "JWT auth validate function is not specified. Use jwt { validate { ... } } to fix."
            )
        }

        fun validate(validate: suspend ApplicationCall.(JWTCredential) -> Principal?) {
            authenticationFunction = validate
        }

        internal fun build() = KeycloakAuthenticationProvider(this)
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        throw NotImplementedError(
            "JWT auth validate function is not specified. Use jwt { validate { ... } } to fix."
        )
    }
}
