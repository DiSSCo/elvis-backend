package org.synthesis.account.registration

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import kotlinx.coroutines.flow.toList
import org.koin.ktor.ext.inject
import org.synthesis.account.*
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveFromParameters
import org.synthesis.infrastructure.ktor.receiveValidated
import org.synthesis.infrastructure.ktor.respondCreated
import org.synthesis.institution.InstitutionId

fun Route.registrationRoutes() {

    val registrationHandler by inject<RegistrationHandler>()
    val userAccountFinder by inject<UserAccountFinder>()

    post("/registration") {
        try {

            val request = call.receiveValidated<RegistrationRequest>()

            val id = registrationHandler.handle(
                data = UserAccountRegistrationData(
                    email = request.email,
                    groups = listOf("requester"),
                    fullName = UserFullName(
                        firstName = request.firstName,
                        lastName = request.lastName
                    ),
                    attributes = UserAccountAttributes(
                        orcId = request.orcId?.let { OrcId(it) },
                        relatedInstitutionId = request.relatedInstitutionId?.let { InstitutionId.fromString(it) },
                        gender = request.gender?.let { Gender.valueOf(it.uppercase()) } ?: Gender.OTHER,
                        birthDate = request.birthDate?.let { LocalDate.parse(it) },
                        nationality = request.nationality.toString(),
                        homeInstitutionId = request.homeInstitutionId.toString(),
                        countryOtherInstitution = request.countryOtherInstitution.toString(),
                    ),
                    credentials = UserAccountRegistrationCredentials.ClearPassword(request.password)
                ),
                withNotification = true
            )

            call.respondCreated("User successfully registered", mapOf("id" to id))
        } catch (e: UserAccountException.AlreadyRegistered) {
            throw IncorrectRequestParameters.create(
                "email",
                "User with the specified address already exists"
            )
        }
    }

    get("/registration/{email}") {
        val users = userAccountFinder.find(
            query = call.receiveFromParameters("email"),
            offset = 0,
            limit = 100
        ).toList()

        call.respond(
            mapOf("exists" to users.isNotEmpty())
        )
    }
}
