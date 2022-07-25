package org.synthesis.reporting

import io.ktor.application.call
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.flow.toList
import org.koin.ktor.ext.inject
import org.synthesis.auth.interceptor.withRole

@Suppress("LongMethod")
fun Route.reportingRoutes() {
    val presenter by inject<ReportingPresenter>()
    withRole("reporting_view") {
        get("/reporting/requests/{callId}/{type}/{group}") {
            val result = presenter.getReportingByRequests(call.callId(), call.type(), call.group()).toList()
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
