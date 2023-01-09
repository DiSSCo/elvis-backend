package org.synthesis.search

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.synthesis.auth.AuthorizationService
import org.synthesis.auth.interceptor.isGranted
import org.synthesis.auth.ktor.withRole
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveValidated
import org.synthesis.infrastructure.ktor.respondSuccess
import org.synthesis.keycloak.api.Permission
import org.synthesis.keycloak.api.Scope

@Suppress("LongMethod")
fun Route.searchRoutes() {
    val authorizationService by inject<AuthorizationService>()
    val searchProviderLocator by inject<SearchProviderLocator>()

    authenticate {
        withRole("authificated") {}
        post("/search") {
            try {
                val request = call.receiveValidated<SearchRequest>()

                isGranted(
                    authorizationService,
                    Permission("search:${request.index}", Scope("execute"))
                ) {
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

