package org.synthesis.calls.request.ta.store

import io.vertx.pgclient.PgPool
import java.time.LocalDateTime
import org.synthesis.account.UserAccountId
import org.synthesis.calls.request.ta.TaCallRequest
import org.synthesis.calls.store.CallRequestStore
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.infrastructure.persistence.querybuilder.*
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.infrastructure.serializer.Serializer
import org.synthesis.institution.InstitutionId

class TaCallRequestStore(
    private val sqlClient: PgPool,
    private val serializer: Serializer = JacksonSerializer
) : CallRequestStore<TaCallRequest> {
    override suspend fun add(request: TaCallRequest) {
        sqlClient.execute(
            insert(
                to = "requests",
                rows = mapOf(
                    "id" to request.id().uuid,
                    "call_id" to request.callId().uuid,
                    "form" to serializer.serialize(DynamicForm(request.content().general.values)),
                    "created_at" to request.createdAt(),
                    "requester_id" to request.authorId().uuid,
                    "status" to request.status().name.toLowerCase(),
                    "title" to request.title(),
                    "deleted_at" to request.deletedAt(),
                    "resource_id" to request.resourceId()?.id,
                    "type" to "Transnational Access"
                )
            )
        )
    }

    override suspend fun save(request: TaCallRequest) {
        sqlClient.transaction {
            execute(
                update(
                    on = "requests",
                    rows = mapOf(
                        "form" to serializer.serialize(DynamicForm(request.content().general.values)),
                        "status" to request.status().name.toLowerCase(),
                        "title" to request.title(),
                        "country_code" to request.country()?.isoCode?.id,
                        "deleted_at" to request.deletedAt()
                    )
                ) {
                    where { "id" eq request.id().uuid }
                }
            )

            for (scoringForm in request.content().score.values) {
                val serializedScoreForm = serializer.serialize(scoringForm.content())

                execute(
                    insert(
                        to = "scoring_form",
                        rows = mapOf(
                            "id" to scoringForm.id(),
                            "scorer_id" to scoringForm.scorer().id.uuid,
                            "form" to serializedScoreForm,
                            "request_id" to scoringForm.requestId().uuid,
                            "deleted_at" to scoringForm.deletedAt()
                        )
                    ) {
                        onConflict(
                            columns = listOf("request_id", "scorer_id"),
                            action = OnConflict.DoUpdate(
                                rows = mapOf(
                                    "form" to serializedScoreForm,
                                    "deleted_at" to scoringForm.deletedAt()
                                )
                            )
                        )
                    }
                )
            }

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
                            "status" to institutionForm.status().name.toLowerCase(),
                            "deleted_at" to institutionForm.deletedAt(),
                            "type" to "Transnational Access"
                        )
                    ) {
                        onConflict(
                            columns = listOf("request_id", "institution_id"),
                            action = OnConflict.DoUpdate(
                                rows = mapOf(
                                    "form" to serializedInstituteForm,
                                    "status" to institutionForm.status().name.toLowerCase(),
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
                        "type" eq "Transnational Access"
                    }
                }
            )
        }
    }
}
