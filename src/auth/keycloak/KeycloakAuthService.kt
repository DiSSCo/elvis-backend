package org.synthesis.auth.keycloak

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URL
import java.security.PublicKey
import java.time.Duration
import org.keycloak.TokenVerifier
import org.keycloak.jose.jwk.JSONWebKeySet
import org.keycloak.jose.jwk.JWKParser
import org.keycloak.representations.AccessToken
import org.keycloak.util.TokenUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.synthesis.auth.AuthException
import org.synthesis.auth.AuthService
import org.synthesis.infrastructure.cache.BadExpiringAsyncReadCache
import org.synthesis.keycloak.KeycloakConfiguration
import org.synthesis.keycloak.KeycloakConfiguration.Companion.certificateUrl
import org.synthesis.keycloak.KeycloakConfiguration.Companion.realmUrl
import org.synthesis.keycloak.KeycloakRealm

class KeycloakAuthService(
    private val config: KeycloakConfiguration
) : AuthService {
    private val log: Logger = LoggerFactory.getLogger("auth")

    /**
     * Public keys on the Keycloak server are periodically changed (updated).
     * In order not to access the Keycloak every time to check the correctness of the token,
     * it is necessary to cache the received key.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private val cache = BadExpiringAsyncReadCache<KeycloakRealm, JSONWebKeySet>(
        {
            ObjectMapper().readValue(URL(config.certificateUrl()).openStream(), JSONWebKeySet::class.java)
        },
        Duration.ofHours(4)
    )

    override suspend fun verifyToken(accessToken: String): Boolean = try {
        val tokenVerifier = TokenVerifier.create(accessToken, AccessToken::class.java)

        tokenVerifier.withChecks(
            TokenVerifier.RealmUrlCheck(config.realmUrl()),
            TokenVerifier.SUBJECT_EXISTS_CHECK,
            TokenVerifier.TokenTypeCheck(TokenUtil.TOKEN_TYPE_BEARER),
            TokenVerifier.IS_ACTIVE
        )

        val publicKey = extractPublicKey(tokenVerifier.header.keyId) ?: throw AuthException.NoPublicKeyFound()

        tokenVerifier.publicKey(publicKey)

        tokenVerifier.verify().token != null
    } catch (cause: Throwable) {
        log.trace("failed to verify token: ${cause.message}")

        false
    }

    /**
     * Retrieves the public key stored for the specified realm
     */
    private suspend fun extractPublicKey(keyId: String): PublicKey? =
        cache.get(config.activeRealm)
            ?.keys
            ?.firstOrNull { it.keyId == keyId }
            ?.let { JWKParser.create(it).toPublicKey() }
}
