package org.synthesis.institution.coordinator

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.flow.toList
import org.koin.ktor.ext.inject
import org.synthesis.account.manage.userAccountId
import org.synthesis.auth.interceptor.authenticatedUser
import org.synthesis.auth.interceptor.withRole
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveFromParameters
import org.synthesis.infrastructure.ktor.respondCollection
import org.synthesis.infrastructure.ktor.respondSuccess
import org.synthesis.institution.institutionId

@Suppress("LongMethod")
fun Route.manageInstitutionRoles() {

    val coordinatorManager by inject<CoordinatorManager>()

    val rolesMapping = mapOf(
        "institution_moderator" to "institution moderator",
        "ta_coordinator" to "ta coordinator",
        "va_coordinator" to "va coordinator"
    )

    fun ApplicationCall.extractRole(): String {
        val requestedRoute = receiveFromParameters("role")
        return rolesMapping[requestedRoute] ?: throw IncorrectRequestParameters.create(
            "role",
            "Incorrect `role` value ($requestedRoute)"
        )
    }

    fun ApplicationCall.extractAction(): String {
        val expectedActions = listOf("add", "remove")
        val requestedAction = receiveFromParameters("action")

        if (expectedActions.contains(requestedAction)) {
            return requestedAction
        }

        throw IncorrectRequestParameters.create("action", "Incorrect action provided")
    }

    fun PipelineContext<*, ApplicationCall>.assertCanManagePersonal() {
        if ("administrator" in authenticatedUser().groups) {
            return
        }

        val assignedInstitution = authenticatedUser().attributes.institutionId

        if (assignedInstitution != null && assignedInstitution.grid.value == call.institutionId().grid.value) {
            return
        }

        throw IncorrectRequestParameters.create(
            "institutionId",
            "You cannot edit personal for someone else's institution"
        )
    }

    withRole("manage_coordinators") {

        route("/institutions/{institutionId}/{role}") {

            /**
             * List of coordinators.
             */
            get {
                assertCanManagePersonal()

                call.respondCollection(
                    coordinatorManager.list(call.extractRole(), call.institutionId()).toList()
                )
            }

            /**
             * Add/remove coordinator.
             */
            post("/{action}/{userAccountId}") {
                assertCanManagePersonal()

                val role = call.extractRole()
                val userId = call.userAccountId()
                val institutionId = call.institutionId()

                when (call.extractAction()) {
                    "add" -> coordinatorManager.promote(userId, role, institutionId)
                    "remove" -> coordinatorManager.demote(userId, role)
                }

                call.respondSuccess()
            }
        }
    }
}
