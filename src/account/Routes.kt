package org.synthesis.account

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate
import org.koin.ktor.ext.inject
import org.synthesis.account.manage.UpdateUserAccountRequest
import org.synthesis.account.manage.UserAccountProvider
import org.synthesis.account.registration.UpdateProfileRequest
import org.synthesis.auth.interceptor.authenticatedUser
import org.synthesis.auth.interceptor.withRole
import org.synthesis.infrastructure.ktor.receiveValidated
import org.synthesis.infrastructure.ktor.respondSuccess
import org.synthesis.institution.InstitutionId

fun Route.profileRoutes() {

    val userAccountFinder by inject<UserAccountFinder>()
    val userAccountProvider by inject<UserAccountProvider>()

    withRole("authificated") {

        get("/profile") {
            call.respond(
                userAccountFinder.find(authenticatedUser().id)!!.asView()
            )
        }

        post("/profile") {
            val currentUser = authenticatedUser()
            val request = call.receiveValidated<UpdateProfileRequest>()

            userAccountProvider.update(
                UpdateUserAccountRequest(
                    id = currentUser.id,
                    fullName = UserFullName(request.firstName, request.lastName),
                    attributes = currentUser.attributes.copy(
                        gender = request.gender?.let { Gender.valueOf(it.uppercase()) } ?: Gender.OTHER,
                        relatedInstitutionId = request.relatedInstitutionId?.let { InstitutionId.fromString(it) },
                        orcId = request.orcId?.let { OrcId(it) },
                        birthDate = request.birthDate?.let { LocalDate.parse(it) },
                        homeInstitutionId = request.homeInstitutionId,
                        nationality = request.nationality,
                        countryOtherInstitution = request.countryOtherInstitution
                    )
                )
            )

            call.respondSuccess()
        }
    }
}
