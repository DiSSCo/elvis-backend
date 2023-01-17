package org.synthesis.reporting

import kotlinx.coroutines.flow.Flow

data class ReportingParameter(
    val callId: String? = null,
    val type: String? = null,
    val group: String? = null,
    val role: String? = null
)

class ReportingPresenter(
    private val reportingReceiver: ReportingReceiver
) {
    fun getReportingByRequests(callId: ReportingParameter, type: ReportingParameter, group: ReportingParameter): Flow<FormatOne> {
        return reportingReceiver.getReportingByRequests(callId, type, group)
    }

    fun getReportingByCountry(callId: ReportingParameter, group: ReportingParameter): Flow<FormatTwo> {
        return reportingReceiver.getReportingByCountry(callId, group)
    }

    fun getReportingByRequesters(callId: ReportingParameter, type: ReportingParameter): Flow<FormatThree> {
        return reportingReceiver.getReportingByRequesters(callId, type)
    }

    fun getReportingByRole(role: ReportingParameter): Flow<FormatFour> {
        return reportingReceiver.getReportingByRole(role)
    }
}
