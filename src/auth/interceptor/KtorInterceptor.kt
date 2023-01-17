package org.synthesis.auth.interceptor

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import org.synthesis.account.UserAccount
import org.synthesis.auth.AuthException
import org.synthesis.auth.AuthorizationService
import org.synthesis.keycloak.api.Permission

/**
 * Getting information about the current authenticated user
 *
 * @throws [AuthException.IncorrectAuthToken]
 */
fun PipelineContext<*, ApplicationCall>.authenticatedUser(): UserAccount =
    call.principal() ?: throw AuthException.IncorrectAuthToken()

/**
 * Checks the ability for the specified token to perform an operation.
 *
 * @todo: Remove [authorizationService] argument
 *
 * @throws [AuthException.NotAllowed]
 */
suspend fun <R> PipelineContext<*, ApplicationCall>.isGranted(
    authorizationService: AuthorizationService,
    vararg permissions: Permission,
    code: suspend PipelineContext<*, ApplicationCall>.() -> R
): R {
    val token = call.request.headers["Authorization"] ?: ""

    val authorized = authorizationService.isGranted(token, *permissions)

    return if (authorized) {
        code()
    } else {
        throw AuthException.NotAllowed(*permissions)
    }
}
