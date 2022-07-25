package org.synthesis

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.koin.ktor.ext.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.*
import org.synthesis.account.manage.userManageRoutes
import org.synthesis.account.profileRoutes
import org.synthesis.account.registration.registrationRoutes
import org.synthesis.auth.AuthException
import org.synthesis.auth.authRoutes
import org.synthesis.auth.ktor.KtorAuthConfigurer
import org.synthesis.calls.callRoutes
import org.synthesis.contact.contactRoutes
import org.synthesis.country.countryRoutes
import org.synthesis.infrastructure.ApiResponse
import org.synthesis.infrastructure.ApplicationException
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.respondBadRequest
import org.synthesis.institution.coordinator.manageInstitutionRoles
import org.synthesis.institution.institutionRoutes
import org.synthesis.reporting.reportingRoutes
import org.synthesis.search.searchRoutes
import org.synthesis.settings.settingsRoutes

@InternalAPI
@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
@Suppress("LongMethod")
fun Application.module() {

    val authConfigurer by inject<KtorAuthConfigurer>()
    val logger: Logger = LoggerFactory.getLogger("app")

    install(Authentication) {
        with(authConfigurer) {
            configure()
        }
    }

    install(CORS) {
        anyHost()
        allowCredentials = true
        allowNonSimpleContentTypes = true
        header("access-control-allow-origin")
        header("authorization")
        exposeHeader("Content-Disposition")
    }

    install(Locations)
    install(DataConversion)

    install(CallLogging) {
        this.logger = logger
        this.level = Level.DEBUG
    }

    install(StatusPages) {
        exception<IncorrectRequestParameters> { cause ->
            logger.error(cause.message.orEmpty(), cause)

            call.respondBadRequest("Request validation failed", cause.violations)
        }
        exception<AuthException.NotAllowed> { cause ->
            logger.error(cause.message.orEmpty(), cause)

            call.respond(HttpStatusCode.Forbidden)
        }
        exception<AuthException> { cause ->
            logger.error(cause.message.orEmpty(), cause)

            call.respondBadRequest("Incorrect auth token", mapOf("token" to cause.message))
        }
        exception<Throwable> { cause ->

            logger.error(cause.message.orEmpty(), cause)

            val message = if (cause is ApplicationException) cause.message else "An internal error has occurred"

            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse.Fail(description = message)
            )
        }
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }

    routing {
        authRoutes()
        profileRoutes()
        institutionRoutes()
        manageInstitutionRoles()
        contactRoutes()
        searchRoutes()
        settingsRoutes()
        userManageRoutes()
        registrationRoutes()
        callRoutes()
        countryRoutes()
        reportingRoutes()
    }
}
