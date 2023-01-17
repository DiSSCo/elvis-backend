package org.synthesis

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.dataconversion.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.*
import org.synthesis.account.UserAccount
import org.synthesis.account.manage.userManageRoutes
import org.synthesis.account.profileRoutes
import org.synthesis.account.registration.registrationRoutes
import org.synthesis.auth.AuthException
import org.synthesis.auth.authRoutes
import org.synthesis.auth.ktor.*
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

@Suppress("LongMethod")
fun Application.module() {

     val logger: Logger = LoggerFactory.getLogger("app")

    configureSecurity()

    install(CORS) {
        anyHost()
        allowCredentials = true
        allowNonSimpleContentTypes = true
        allowHeader("access-control-allow-origin")
        allowHeader("authorization")
        exposeHeader("Content-Disposition")
    }

    install(Locations)
    install(DataConversion)

    install(CallLogging) {
        this.logger = logger
        this.level = Level.DEBUG
    }

    install(StatusPages) {
        exception<IncorrectRequestParameters> { call, cause ->
            logger.error(cause.message.orEmpty(), cause)

            call.respondBadRequest("Request validation failed", cause.violations)
        }
        exception<AuthException.NotAllowed> { call, cause ->
            logger.error(cause.message.orEmpty(), cause)

            call.respond(HttpStatusCode.Forbidden)
        }
        exception<AuthException> { call, cause ->
            logger.error(cause.message.orEmpty(), cause)

            call.respondBadRequest("Incorrect auth token", mapOf("token" to cause.message))
        }
        exception<Throwable> { call, cause ->

            logger.error(cause.message.orEmpty(), cause)

            val message =
                if (cause is ApplicationException) cause.message else "An internal error has occurred"

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

    installRoleBasedAuthPlugin {
        extractRoles { principal ->
            (principal as UserAccount).roles.toSet()
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
