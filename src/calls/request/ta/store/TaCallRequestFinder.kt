package org.synthesis.calls.request.ta.store

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.synthesis.account.UserAccountId
import org.synthesis.calls.CallId
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.ta.TaCallRequest
import org.synthesis.calls.store.CallRequestFinder
import org.synthesis.country.CountryCode
import org.synthesis.country.CountryFinder
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.infrastructure.persistence.querybuilder.fetchAll
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.infrastructure.serializer.Serializer
import org.synthesis.institution.GRID
import org.synthesis.institution.InstitutionId
import org.synthesis.institution.ScorerData
import org.synthesis.keycloak.api.KeycloakResourceId

class TaCallRequestFinder(
    private val sqlClient: SqlClient,
    private val serializer: Serializer = JacksonSerializer,
    private val countryFinder: CountryFinder
) : CallRequestFinder<TaCallRequest> {

    override suspend fun find(requestId: CallRequestId): TaCallRequest? {
        val callRequestRow = sqlClient.fetchOne(
            select("requests") {
                where {
                    "id" eq requestId.uuid
                    "deleted_at" eq null
                }
            }
        ) ?: return null

        val institutionFormCollectionQuery = select("requests_institution_forms") {
            where {
                "request_id" eq requestId.uuid
                "deleted_at" eq null
            }

            "created_at" orderBy "ASC"
        }

        val institutionFormCollection = sqlClient.fetchAll(institutionFormCollectionQuery).map {
            TaCallRequest.InstitutionForm(
                institutionId = InstitutionId(GRID(it.getString("institution_id"))),
                coordinatorId = UserAccountId(it.getUUID("coordinator_id")),
                form = serializer.unserialize(it.getString("form"), DynamicForm::class.java),
                status = TaCallRequest.Status.valueOf(it.getString("status").uppercase()),
                deletedAt = it.getLocalDateTime("deleted_at"),
                id = it.getUUID("id")
            )
        }

        return callRequestRow.hydrate(institutionFormCollection.toList())
    }

    private suspend fun findScoringForms(id: CallRequestId) = sqlClient.fetchAll(
        select(
            from = "scoring_form as sf",
            columns = listOf(
                "sf.id as id",
                "sf.form as form",
                "sf.deleted_at as deleted_at",
                "a.id as user_id",
                "a.email as email",
                "a.first_name as first_name",
                "a.last_name as last_name",
                "a.country_code as country_code"
            )
        ) {
            "accounts a" innerJoin "sf.scorer_id = a.id"

            where {
                "sf.request_id" eq id.uuid
            }
        }
    ).map {
        TaCallRequest.Scoring(
            id = it.getUUID("id"),
            scorer = ScorerData(
                id = UserAccountId(it.getUUID("user_id")),
                email = it.getString("email"),
                firstName = it.getString("first_name"),
                lastName = it.getString("last_name"),
                country = CountryCode(it.getString("country_code"))
            ),
            requestId = id,
            form = serializer.unserialize(it.getString("form"), DynamicForm::class.java),
            deletedAt = it.getLocalDateTime("deleted_at")
        )
    }

    private suspend fun Row.hydrate(institutions: List<TaCallRequest.InstitutionForm>): TaCallRequest {

        val requestId = CallRequestId(getUUID("id"))

        return TaCallRequest(
            id = CallRequestId(getUUID("id")),
            callId = CallId(getUUID("call_id")),
            createdAt = getLocalDateTime("created_at"),
            authorId = UserAccountId(getUUID("requester_id")),
            content = TaCallRequest.ContentForm(
                general = serializer.unserialize(getString("form"), DynamicForm::class.java),
                institutions = institutions.associateBy { it.institutionId() }.toMutableMap(),
                score = findScoringForms(requestId)
                    .toList()
                    .map {
                        it.id() to it
                    }
                    .toMap()
                    .toMutableMap()
            ),
            status = TaCallRequest.Status.valueOf(getString("status").uppercase()),
            deletedAt = getLocalDateTime("deleted_at"),
            resourceId = getUUID("resource_id")?.let { KeycloakResourceId(it) },
            country = getString("country_code")?.let {
                countryFinder.find(CountryCode(it))
            }
        )
    }
}
