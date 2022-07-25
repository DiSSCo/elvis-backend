@file:Suppress("MatchingDeclarationName", "KDocUnresolvedReference")

package org.synthesis.auth.ktor

import com.auth0.jwt.JWT
import com.auth0.jwt.impl.JWTParser
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Payload
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.auth.*
import io.ktor.util.*
import java.util.*

val authTokenAttributeKey = AttributeKey<HttpAuthHeader>("KeycloakJWTAuth")

/**
 * Authentication configuration using Keycloak service
 */
fun Authentication.Configuration.keycloak(configure: KeycloakAuthenticationProvider.Configuration.() -> Unit) {
    val provider = KeycloakAuthenticationProvider.Configuration().apply(configure).build()

    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        try {
            val token = provider.authHeader(call) ?: return@intercept

            call.attributes.put(authTokenAttributeKey, token)

            val principal = try {
                call.verifyToken(
                    token = token,
                    verifier = provider.verifier,
                    validate = provider.authenticationFunction
                )
            } catch (e: Exception) {
                provider.logger.trace("Token verification failed: ${e.message}")

                null
            }

            if (principal !== null) {
                context.principal(principal)
            } else {
                provider.logger.trace("No principal for request")
            }
        } catch (e: Exception) {
            context.error(
                "KeycloakJWTAuth",
                AuthenticationFailedCause.Error(e.message ?: e.javaClass.simpleName)
            )
        }
    }

    register(provider)
}

/**
 * Token verification.
 *
 * The token is checked for correctness (including by accessing the Keycloak server).
 * In case the token is correct, a Principal object is created, which contains all the information about the
 * authenticated user. If the check fails, null is returned.
 */
private suspend fun ApplicationCall.verifyToken(
    token: HttpAuthHeader,
    verifier: suspend ApplicationCall.(accessToken: String) -> Boolean,
    validate: suspend ApplicationCall.(JWTCredential) -> Principal?
): Principal? {

    /**
     * Checks the correctness of the token (by contacting the Keycloak server) and, if successful,
     * returns a string containing the token.
     * Otherwise returns null.
     *
     * @see KeycloakAuthService
     */
    val jwtString = token.blob()?.let {
        if (verifier(this, it)) {
            it
        } else {
            null
        }
    } ?: return null

    val jwt = JWT.decode(jwtString)
    val payload = jwt.parsePayload()
    val credentials = JWTCredential(payload)

    return validate(this, credentials)
}

/**
 * Retrieves the token value from the headers.
 * Currently only Bearer token is supported.
 */
private fun HttpAuthHeader.blob() = if (this is HttpAuthHeader.Single) {
    blob
} else {
    null
}

/**
 * Creating a class containing authentication parameters.
 */
class JWTCredential(val payload: Payload) : Credential

/**
 * Decoding the token.
 */
private fun DecodedJWT.parsePayload() = JWTParser()
    .parsePayload(
        String(
            Base64
                .getUrlDecoder()
                .decode(payload)
        )
    )
