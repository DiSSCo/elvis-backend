package org.synthesis.keycloak.api

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import java.util.*
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.Configuration
import org.slf4j.Logger
import org.synthesis.keycloak.KeycloakConfiguration
import org.synthesis.keycloak.KeycloakConfiguration.Companion.tokenUrl
import org.synthesis.keycloak.KeycloakExceptions

interface KeycloakClient {

    @Suppress("MaxLineLength")
    companion object AuthorizationProperties {
        /**
         *  A string indicating the format of the token specified in the claim_token parameter
         */
        const val grantType = "urn:ietf:params:oauth:grant-type:uma-ticket"

        /**
         *  string value indicating how the server should respond to authorization requests. This parameter is
         *  specially useful when you are mainly interested in either the overall decision or the permissions granted
         *  by the server, instead of a standard OAuth2 response. Possible values are:
         *
         *   - decision: Indicates that responses from the server should only represent the overall decision by returning a JSON
         *   - permissions: Indicates that responses from the server should contain any permission granted by the server by returning a JSON
         */
        const val responseMode = "decision"
    }

    /**
     * Checks if the specified action is available for the current token owner.
     *
     * @throws [KeycloakExceptions.OperationFailed]
     */
    suspend fun allowed(token: String, vararg permissions: Permission): Boolean

    /**
     * Create a specified Resource.
     *
     * @throws [KeycloakExceptions.EntityAlreadyExists]
     * @throws [KeycloakExceptions.OperationFailed]
     */
    suspend fun create(resource: Resource): KeycloakResourceId
}

data class DefaultKeycloakClient(
    private val configuration: KeycloakConfiguration,
    private val client: HttpClient,
    private val logger: Logger
) : KeycloakClient {

    private val authzClient by lazy {
        AuthzClient.create(
            Configuration(
                configuration.serverUrl,
                configuration.activeRealm.value,
                configuration.uiClientInfo.id,
                mapOf(),
                null
            )
        )
    }

    override suspend fun allowed(token: String, vararg permissions: Permission): Boolean = try {
        val response = client.submitForm<String>(
            url = configuration.tokenUrl(),
            formParameters = Parameters.build {
                set("grant_type", KeycloakClient.grantType)
                set("audience", configuration.uiClientInfo.id)
                set("response_mode", KeycloakClient.responseMode)

                permissions.forEach {
                    append("permission", it.toString())
                }
            }
        ) {
            header("Authorization", token)
        }

        /** @todo: fix me */
        val isSuccess = response == "{\"result\":true}"

        isSuccess.also {
            if (!it) {
                logger.error(
                    "The action is prohibited for this token (expected permissions ${permissions.toList()})",
                    response
                )
            }
        }
    } catch (e: Exception) {
        throw KeycloakExceptions.OperationFailed(configuration.activeRealm, e)
    }

    override suspend fun create(resource: Resource): KeycloakResourceId = authzClient.withCatch {
        val response = protection().resource().create(resource.toKeycloakRepresentation())

        response.id?.let { KeycloakResourceId(UUID.fromString(it)) }
            ?: throw Exception("Unable to create resource `${resource.name}`")
    }

    private suspend fun <R> AuthzClient.withCatch(code: suspend AuthzClient.() -> R): R = try {
        code()
    } catch (e: Exception) {

        val realm = this@DefaultKeycloakClient.configuration.activeRealm

        if (e.cause?.message?.contains("Conflict") == true) {
            throw KeycloakExceptions.EntityAlreadyExists(e.message ?: "", realm)
        }

        throw KeycloakExceptions.OperationFailed(realm, e)
    }
}
