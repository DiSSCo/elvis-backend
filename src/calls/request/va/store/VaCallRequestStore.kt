package org.synthesis.calls.request.va.store

import io.vertx.pgclient.PgPool
import java.time.LocalDateTime
import org.synthesis.account.UserAccountId
import org.synthesis.calls.request.va.VaCallRequest
import org.synthesis.calls.store.CallRequestStore
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.infrastructure.persistence.querybuilder.*
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.infrastructure.serializer.Serializer
import org.synthesis.institution.InstitutionId

class VaCallRequestStore(
    private val sqlClient: PgPool,
    private val serializer: Serializer = JacksonSerializer
) : CallRequestStore<VaCallRequest> {

    override suspend fun add(request: VaCallRequest) {
        sqlClient.execute(
            insert(
                to = "requests",
                rows = mapOf(
                    "id" to request.id().uuid,
                    "call_id" to request.callId().uuid,
                    "form" to serializer.serialize(DynamicForm(request.content().general.values)),
                    "created_at" to request.createdAt(),
                    "requester_id" to request.authorId().uuid,
                    "status" to request.status().name.lowercase(),
                    "title" to request.title(),
                    "deleted_at" to request.deletedAt(),
                    "resource_id" to request.resourceId()?.id,
                    "type" to "Virtual Access"
                )
            )
        )
    }

    override suspend fun save(request: VaCallRequest) {
        sqlClient.transaction {
            execute(
                update(
                    on = "requests",
                    rows = mapOf(
                        "form" to serializer.serialize(DynamicForm(request.content().general.values)),
                        "status" to request.status().name.lowercase(),
                        "title" to request.title(),
                        "deleted_at" to request.deletedAt()
                    )
                ) {
                    where { "id" eq request.id().uuid }
                }
            )

            for (institutionForm in request.content().institutions.values) {
                val serializedInstituteForm = serializer.serialize(institutionForm.content())

                execute(
                    insert(
                        to = "requests_institution_forms",
                        rows = mapOf(
                            "id" to institutionForm.id(),
                            "request_id" to request.id().uuid,
                            "institution_id" to institutionForm.institutionId().grid.value,
                            "coordinator_id" to institutionForm.coordinatorId().uuid,
                            "created_at" to LocalDateTime.now(),
                            "form" to serializedInstituteForm,
                            "status" to institutionForm.status().name.lowercase(),
                            "deleted_at" to institutionForm.deletedAt(),
                            "type" to "Virtual Access"
                        )
                    ) {
                        onConflict(
                            columns = listOf("request_id", "institution_id"),
                            action = OnConflict.DoUpdate(
                                rows = mapOf(
                                    "form" to serializedInstituteForm,
                                    "status" to institutionForm.status().name.lowercase(),
                                    "deleted_at" to institutionForm.deletedAt()
                                )
                            )
                        )
                    }
                )
            }
        }
    }

    override suspend fun updateCoordinator(coordinatorId: UserAccountId, institutionId: InstitutionId) {
        sqlClient.transaction {

            execute(
                update(
                    on = "requests_institution_forms",
                    rows = mapOf(
                        "coordinator_id" to coordinatorId.uuid
                    )
                ) {
                    where {
                        "institution_id" eq institutionId.grid.value
                        "type" eq "Virtual Access"
                    }
                }
            )
        }
    }
}
