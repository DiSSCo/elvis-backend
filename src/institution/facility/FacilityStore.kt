package org.synthesis.institution.facility

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.toList
import org.synthesis.account.UserAccountId
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.formbuilder.FieldValue
import org.synthesis.formbuilder.GroupId
import org.synthesis.infrastructure.persistence.querybuilder.*
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.infrastructure.serializer.Serializer
import org.synthesis.institution.GRID
import org.synthesis.institution.InstitutionId

interface FacilityStore {
    suspend fun add(facility: Facility)
    suspend fun save(facility: Facility)
    suspend fun load(facilityId: FacilityId): Facility?
    suspend fun loadByInstitutionId(institutionId: InstitutionId): List<Facility?>
}

class PgFacilityStore(
    private val sqlClient: SqlClient,
    private val jacksonSerializer: Serializer = JacksonSerializer
) : FacilityStore {
    override suspend fun add(facility: Facility) {
        sqlClient.execute(
            insert(
                "institution_facilities", mapOf(
                    "id" to facility.id.uuid,
                    "moderator_id" to facility.moderatorId.uuid,
                    "created_at" to facility.createdAt,
                    "institution_id" to facility.institutionId.grid.value,
                    "data" to jacksonSerializer.serialize(facility.form),
                    "title" to facility.title(),
                    "title_local" to facility.localTitle(),
                    "images" to facility.images().toTypedArray(),
                    "instruments" to facility.extractServices()
                )
            )
        )
    }

    override suspend fun save(facility: Facility) {
        sqlClient.execute(
            update(
                "institution_facilities", mapOf(
                    "data" to jacksonSerializer.serialize(DynamicForm(facility.form.values)),
                    "deleted_at" to facility.deletedAt,
                    "title" to facility.title(),
                    "title_local" to facility.localTitle(),
                    "images" to facility.images().toTypedArray(),
                    "instruments" to facility.extractServices()
                )
            ) {
                where { "id" eq facility.id.uuid }
            }
        )
    }

    override suspend fun load(facilityId: FacilityId): Facility? = sqlClient.fetchOne(
        select("institution_facilities") {
            where { "id" eq facilityId.uuid }
        }
    )?.hydrate()

    override suspend fun loadByInstitutionId(institutionId: InstitutionId): List<Facility?> = sqlClient.fetchAll(
        select("institution_facilities") {
            where { "institution_id" eq institutionId.grid.value }
        }
    ).toList().map { it.hydrate() }

    private fun Row.hydrate() =
        Facility(
            id = FacilityId(getUUID("id")),
            institutionId = InstitutionId(GRID(getString("institution_id"))),
            moderatorId = UserAccountId(getUUID("moderator_id")),
            deletedAt = getLocalDateTime("deleted_at"),
            form = jacksonSerializer.unserialize(getString("data"), DynamicForm::class.java),
            createdAt = getLocalDateTime("created_at"),
            images = getArrayOfStrings("images").toMutableList()
        )

    private fun Facility.extractServices(): String = form.groupValues(GroupId("instruments")).mapNotNull {
        (it.value.value as? FieldValue.Text)?.value
    }.joinToString(",")
}
