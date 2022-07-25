package org.synthesis.search

import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.post
import org.koin.ktor.ext.inject
import org.synthesis.auth.AuthorizationService
import org.synthesis.auth.interceptor.isGranted
import org.synthesis.auth.interceptor.withRole
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveValidated
import org.synthesis.infrastructure.ktor.respondSuccess
import org.synthesis.keycloak.api.Permission
import org.synthesis.keycloak.api.Scope

@Suppress("LongMethod")
fun Route.searchRoutes() {
    val authorizationService by inject<AuthorizationService>()
    val searchProviderLocator by inject<SearchProviderLocator>()

    withRole("authificated") {
        post("/search") {
            try {
                val request = call.receiveValidated<SearchRequest>()

                isGranted(authorizationService, Permission("search:${request.index}", Scope("execute"))) {
                    call.respondSuccess(
                        searchProviderLocator.obtain(request.index).handle(request)
                    )
                }
            } catch (e: IncorrectIndex) {
                throw IncorrectRequestParameters.create("index", "Incorrect search index specified")
            }
        }
    }
}
