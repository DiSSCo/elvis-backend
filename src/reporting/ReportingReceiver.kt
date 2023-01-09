package org.synthesis.reporting

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.synthesis.infrastructure.persistence.querybuilder.fetchAll
import org.synthesis.infrastructure.persistence.querybuilder.select

interface ReportingReceiver {
    fun getReportingByRequests(callId: ReportingParameter, type: ReportingParameter, group: ReportingParameter): Flow<FormatOne>
    fun getReportingByCountry(callId: ReportingParameter, group: ReportingParameter): Flow<FormatTwo>
    fun getReportingByRequesters(callId: ReportingParameter, type: ReportingParameter): Flow<FormatThree>
    fun getReportingByRole(role: ReportingParameter): Flow<FormatFour>
}

class PgReportingReceiver(
    private val sqlClient: SqlClient
) : ReportingReceiver {
    override fun getReportingByRequests(
        callId: ReportingParameter,
        type: ReportingParameter,
        group: ReportingParameter
    ): Flow<FormatOne> {
        val query = select(
            from = "calls as c",
            columns = listOf(
                "a.${group.group} as requester_country",
                "COUNT(*) as rows",
                "sum(case when a.gender = 'male' then 1 else 0 end) as male",
                "sum(case when a.gender = 'female' then 1 else 0 end) as female",
                "sum(case when a.gender = 'other' then 1 else 0 end) as other"
            )

        ) {
            where {
                "c.id" eq callId.callId
                "a.${group.group}" notEq "NULL"
                "r.status" eq type.type
            }
            groupBy("a.${group.group}")
            "requests r" leftJoin "r.call_id = c.id"
            "accounts a" leftJoin "r.requester_id = a.id"
            "a.${group.group}" orderBy "ASC"
        }

        val data = sqlClient.fetchAll(query).map {
            it.toFirstFormat()
        }
        return data
    }

    override fun getReportingByCountry(callId: ReportingParameter, group: ReportingParameter): Flow<FormatTwo> {
        val query = select(
            from = "accounts as a",
            columns = listOf(
                "CONCAT(first_name,' ',last_name) as requester_name",
                "email",
                "r.title as request_title",
                "i.title as institution_title",
                "c.end_date - c.start_date as days_of_visit"
            )
        ) {
            where {
                "c.id" eq callId.callId
                "a.country_code" eq "${group.group}"
            }
            "requests r" leftJoin "r.requester_id = a.id"
            "calls c" leftJoin "r.call_id = c.id"
            "institutions i" leftJoin "a.institution_id = i.id"
            "requester_name" orderBy "ASC"
        }

        val data = sqlClient.fetchAll(query).map {
            it.toSecondFormat()
        }
        return data
    }

    override fun getReportingByRequesters(callId: ReportingParameter, type: ReportingParameter): Flow<FormatThree> {
        val query = select(
            from = "accounts as a",
            columns = listOf(
                "CONCAT(first_name,' ',last_name) as requester_name",
                "email",
                "INITCAP(gender) as gender"
            )
        ) {
            where {
                "c.id" eq callId.callId
                "array_to_string(group_list, ',', '*')" lLike "${type.type}"
            }
            "requests r" leftJoin "r.requester_id = a.id"
            "calls c" leftJoin "r.call_id = c.id"
            "requester_name" orderBy "ASC"
        }

        val data = sqlClient.fetchAll(query).map {
            it.toThirdFormat()
        }
        return data
    }

    override fun getReportingByRole(role: ReportingParameter): Flow<FormatFour> {
        val query = select(
            from = "accounts as a",
            columns = listOf(
                "CONCAT(first_name,' ',last_name) as requester_name",
                "array_to_string(group_list, ',', '*')",
                "INITCAP(gender) as gender"
            )
        ) {
            where {
                "array_to_string(group_list, ',', '*')" lLike "${role.role}"
            }
            "requests r" leftJoin "r.requester_id = a.id"
            "calls c" leftJoin "r.call_id = c.id"
            "requester_name" orderBy "ASC"
        }

        val data = sqlClient.fetchAll(query).map {
            it.toFourthFormat()
        }
        return data
    }
}

fun Row.toFirstFormat() = FormatOne(
    requester_country = getString("requester_country"),
    rows = getInteger("rows"),
    male = getInteger("male"),
    female = getInteger("female"),
    other = getInteger("other")
)

fun Row.toSecondFormat() = FormatTwo(
    requester = getString("requester_name"),
    email = getString("email"),
    projectTitle = getString("title"),
    institution = getString("institution"),
    daysOfVisit = getString("days_of_visit")
)

fun Row.toThirdFormat() = FormatThree(
    requester = getString("requester_name"),
    email = getString("email"),
    gender = getString("gender")
)

fun Row.toFourthFormat() = FormatFour(
    requester = getString("requester_name"),
    role = getString("role"),
    gender = getString("gender")
)
