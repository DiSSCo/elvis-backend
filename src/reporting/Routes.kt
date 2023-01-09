package org.synthesis.reporting

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.toList
import org.koin.ktor.ext.inject
import org.synthesis.auth.ktor.withRole

@Suppress("LongMethod")
fun Route.reportingRoutes() {
    val presenter by inject<ReportingPresenter>()
    authenticate {
        withRole("reporting_view") {}
        get("/reporting/requests/{callId}/{type}/{group}") {
            val result =
                presenter.getReportingByRequests(call.callId(), call.type(), call.group()).toList()
            call.respond(HttpStatusCode.OK, result)
        }

        get("/reporting/countries/{callId}/{group}") {
            val result = presenter.getReportingByCountry(call.callId(), call.group()).toList()
            call.respond(HttpStatusCode.OK, result)
        }

        get("/reporting/requesters/{callId}/{type}") {
            val result = presenter.getReportingByRequesters(call.callId(), call.type()).toList()
            call.respond(HttpStatusCode.OK, result)
        }

        get("/reporting/roles/{role}") {
            val result = presenter.getReportingByRole(call.role()).toList()
            call.respond(HttpStatusCode.OK, result)
        }
    }
}
