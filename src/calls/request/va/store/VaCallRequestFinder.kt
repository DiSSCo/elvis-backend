package org.synthesis.calls.request.va.store

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.synthesis.account.UserAccountId
import org.synthesis.calls.CallId
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.va.VaCallRequest
import org.synthesis.calls.store.CallRequestFinder
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.infrastructure.persistence.querybuilder.fetchAll
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.infrastructure.serializer.Serializer
import org.synthesis.institution.GRID
import org.synthesis.institution.InstitutionId
import org.synthesis.keycloak.api.KeycloakResourceId

class VaCallRequestFinder(
    private val sqlClient: SqlClient,
    private val serializer: Serializer = JacksonSerializer
) : CallRequestFinder<VaCallRequest> {

    override suspend fun find(requestId: CallRequestId): VaCallRequest? {
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
            VaCallRequest.InstitutionForm(
                institutionId = InstitutionId(GRID(it.getString("institution_id"))),
                coordinatorId = UserAccountId(it.getUUID("coordinator_id")),
                form = serializer.unserialize(it.getString("form"), DynamicForm::class.java),
                status = VaCallRequest.Status.valueOf(it.getString("status").toUpperCase()),
                deletedAt = it.getLocalDateTime("deleted_at"),
                id = it.getUUID("id")
            )
        }

        return callRequestRow.hydrate(institutionFormCollection.toList())
    }

    private fun Row.hydrate(institutions: List<VaCallRequest.InstitutionForm>) = VaCallRequest(
        id = CallRequestId(getUUID("id")),
        callId = CallId(getUUID("call_id")),
        createdAt = getLocalDateTime("created_at"),
        authorId = UserAccountId(getUUID("requester_id")),
        content = VaCallRequest.ContentForm(
            general = serializer.unserialize(getString("form"), DynamicForm::class.java),
            institutions = institutions.associateBy { it.institutionId() }.toMutableMap()
        ),
        status = VaCallRequest.Status.valueOf(getString("status").uppercase()),
        deletedAt = getLocalDateTime("deleted_at"),
        resourceId = getUUID("resource_id")?.let { KeycloakResourceId(it) }
    )
}
