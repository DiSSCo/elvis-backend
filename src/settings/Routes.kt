package org.synthesis.settings

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.synthesis.auth.ktor.withRole
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveValidated
import org.synthesis.infrastructure.ktor.respondSuccess

fun Route.settingsRoutes() {

    val settingsStore by inject<SettingsStore>()
    val settingsPresenter by inject<SettingsPresenter>()

    route("/admin/settings") {

        fun ApplicationCall.optionKey(): String = parameters["optionKey"]
            ?: throw IncorrectRequestParameters(mapOf("optionKey" to "Parameter must contain correct option key"))
        authenticate {
            method(HttpMethod.Post) {
                withRole("settings_edit") {
                    settingsStore.save(
                        call.receiveValidated<SettingsParameter>().asParameter()
                    )

                    call.respondSuccess()
                }
            }
        }

        get("/{optionKey}") {
            call.respondSuccess(
                settingsPresenter.obtain(call.optionKey())
            )
        }
    }
}
