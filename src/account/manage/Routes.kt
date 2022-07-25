package org.synthesis.account.manage

import io.ktor.application.*
import io.ktor.routing.*
import java.time.LocalDate
import kotlinx.coroutines.flow.toList
import org.koin.ktor.ext.inject
import org.synthesis.account.*
import org.synthesis.account.registration.UserAccountRegistrationCredentials
import org.synthesis.auth.interceptor.withRole
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveValidated
import org.synthesis.infrastructure.ktor.respondCollection
import org.synthesis.infrastructure.ktor.respondCreated
import org.synthesis.infrastructure.ktor.respondSuccess
import org.synthesis.institution.InstitutionId

fun Route.userManageRoutes() {
    val userAccountProvider by inject<UserAccountProvider>()
    val userAccountFinder by inject<UserAccountFinder>()

    route("/users") {
        withRole("manage_users") {

            /**
             * Getting a list of users
             */
            get {

                call.respondCollection(
                    userAccountFinder.find(
                        query = call.parameters["query"] ?: "",
                        offset = call.parameters["offset"]?.toInt() ?: 0,
                        limit = call.parameters["limit"]?.toInt() ?: 100
                    ).toList()
                )
            }

            /**
             * Adding a new user
             */
            post("/add") {
                try {

                    val command = call.receiveValidated<UserManageCommand.Create>()

                    val accountId = userAccountProvider.create(
                        CreateUserAccountRequest(
                            email = command.email,
                            groups = command.groups,
                            fullName = command.fullName,
                            attributes = UserAccountAttributes(
                                orcId = command.attributes.orcId?.let { OrcId(it) },
                                institutionId = command.attributes.institutionId?.let { InstitutionId.fromString(it) },
                                relatedInstitutionId = command.attributes.relatedInstitutionId?.let {
                                    InstitutionId.fromString(
                                        it
                                    )
                                },
                                gender = command.attributes.gender?.let { Gender.valueOf(it.toUpperCase()) }
                                    ?: Gender.OTHER,
                                birthDate = command.attributes.birthDate?.let { LocalDate.parse(it) },
                                nationality = command.attributes.nationality,
                                countryOtherInstitution = command.attributes.countryOtherInstitution
                            ),
                            credentials = UserAccountRegistrationCredentials.ClearPassword(command.password)
                        )
                    )

                    call.respondCreated("User successful registered", mapOf("id" to accountId.uuid))
                } catch (e: UserAccountException.AlreadyRegistered) {
                    throw IncorrectRequestParameters.create("email", e.message ?: "User already exists")
                }
            }

            route("/{userAccountId}") {

                /**
                 * Blocking access for a user
                 */
                post("/ban") {
                    val id = call.userAccountId()
                    val reason = call.receiveValidated<UserManageCommand.Ban>().reason

                    userAccountProvider.ban(id, reason)

                    call.respondSuccess("User successfully banned", mapOf("id" to id))
                }

                /**
                 * Removing access blocking from a user
                 */
                post("/unban") {
                    val id = call.userAccountId()

                    userAccountProvider.unban(id)

                    call.respondSuccess("User unbanned successfully", mapOf("id" to id))
                }
            }
        }
    }
}
