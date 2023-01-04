package org.synthesis.auth.interceptor

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.PipelineContext
import org.synthesis.account.UserAccount
import org.synthesis.auth.AuthException
import org.synthesis.auth.AuthorizationService
import org.synthesis.auth.ktor.authTokenAttributeKey
import org.synthesis.infrastructure.ktor.accessDenied
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
    val token = call.attributes[authTokenAttributeKey].render()

    val authorized = authorizationService.isGranted(token, *permissions)

    return if (authorized) {
        code()
    } else {
        throw AuthException.NotAllowed(*permissions)
    }
}

/**
 * Checks if the user has the specified roles
 */
fun Route.withRole(vararg expectedRoles: String, buildChildRoutes: Route.() -> Unit) = authenticate {
    val authenticatedRoutes = createChild(
        object : RouteSelector(1.0) {
            override fun evaluate(
                context: RoutingResolveContext,
                segmentIndex: Int
            ) = RouteSelectorEvaluation.Constant

            override fun toString() = "(withRoles: ${expectedRoles.joinToString(",")})"
        }
    )

    authenticatedRoutes.intercept(ApplicationCallPipeline.Call) {
        val principal = call.principal<UserAccount>()

        if (principal != null) {
            for (expectedPermission in expectedRoles) {
                if (expectedPermission in principal.roles) {
                    return@intercept proceed()
                }
            }
        }

        accessDenied()
        finish()

        return@intercept
    }

    authenticatedRoutes.buildChildRoutes()
}
