package org.synthesis.country

import io.ktor.application.*
import io.ktor.routing.*
import kotlinx.coroutines.flow.toList
import org.koin.ktor.ext.inject
import org.synthesis.account.manage.userAccountId
import org.synthesis.auth.interceptor.withRole
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveFromParameters
import org.synthesis.infrastructure.ktor.respondCollection
import org.synthesis.infrastructure.ktor.respondSuccess

private val expectedActions = listOf("add", "remove")

fun Route.countryRoutes() {

    val countryManager by inject<CountryManager>()

    route("/countries/{countryIsoCode}") {
        fun ApplicationCall.extractAction(): String {
            val requestedAction = receiveFromParameters("action")

            if (expectedActions.contains(requestedAction)) {
                return requestedAction
            }

            throw IncorrectRequestParameters.create("action", "Incorrect action provided")
        }

        /**
         * TA Scorers list.
         */
        withRole("scorer_manage") {
            get("/ta_scorer") {
                call.respondCollection(countryManager.taScorers(call.countryCode()).toList())
            }
        }

        /**
         * Manage TA Scorers (promote/demote)
         */
        withRole("scorer_manage") {
            post("/ta_scorer/{action}/{userAccountId}") {
                val userAccountId = call.userAccountId()
                val currencyCode = call.countryCode()

                when (call.extractAction()) {
                    "add" -> countryManager.promote(userAccountId, "ta scorer", currencyCode).also {
                        call.respondSuccess("User :$userAccountId Promoted")
                    }
                    "remove" -> countryManager.demote(userAccountId, "ta scorer").also {
                        call.respondSuccess("User :$userAccountId Demoted")
                    }
                }
            }
        }

        /**
         * TAF Admins list.
         */
        withRole("taf_admin_manage") {
            get("/taf_admin") {
                call.respondCollection(countryManager.tafAdmins(call.countryCode()).toList())
            }
        }

        /**
         * Manage TAF Admins (promote/demote)
         */
        withRole("taf_admin_manage") {
            post("/taf_admin/{action}/{userAccountId}") {
                val userAccountId = call.userAccountId()
                val currencyCode = call.countryCode()

                when (call.extractAction()) {
                    "add" -> countryManager.promote(userAccountId, "taf admin", currencyCode).also {
                        call.respondSuccess("User :$userAccountId Promoted")
                    }

                    "remove" -> countryManager.demote(userAccountId, "taf admin").also {
                        call.respondSuccess("User :$userAccountId Demoted")
                    }
                }
            }
        }
    }
}
