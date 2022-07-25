package org.synthesis.institution.facility

import io.ktor.util.*
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import java.time.LocalDateTime
import java.util.*
import org.synthesis.attachment.AttachmentCollection
import org.synthesis.attachment.AttachmentId
import org.synthesis.attachment.AttachmentMimeType
import org.synthesis.attachment.AttachmentProvider
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.infrastructure.serializer.Serializer

data class FacilityResponse(
    val id: UUID,
    val institutionId: String,
    val moderatorId: UUID,
    var deletedAt: LocalDateTime?,
    var form: DynamicForm,
    val createdAt: LocalDateTime,
    val images: List<String> = listOf()
)

data class FacilityImage(
    val contentType: AttachmentMimeType,
    val payload: String
)

interface FacilityPresenter {
    suspend fun find(id: FacilityId): FacilityResponse?
    suspend fun obtainImage(id: FacilityId, imageId: UUID): FacilityImage?
}

class DefaultFacilityPresenter(
    private val sqlClient: SqlClient,
    private val attachmentProvider: AttachmentProvider,
    private val serializer: Serializer = JacksonSerializer
) : FacilityPresenter {
    private val encoder = Base64.getEncoder()

    @InternalAPI
    override suspend fun obtainImage(id: FacilityId, imageId: UUID): FacilityImage? {
        val facility = find(id) ?: return null

        if (imageId.toString() in facility.images) {
            val attachment = attachmentProvider.receive(
                id = AttachmentId(imageId),
                from = AttachmentCollection(id.uuid.toString())
            ) ?: return null

            return FacilityImage(
                payload = encoder.encodeToString(attachment.payload.moveToByteArray()),
                contentType = attachment.metadata.mimeType
            )
        }

        return null
    }

    override suspend fun find(id: FacilityId): FacilityResponse? = sqlClient.fetchOne(
        select("institution_facilities") {
            where { "id" eq id.uuid }
        }
    )?.hydrate()

    private fun Row.hydrate() = FacilityResponse(
        id = getUUID("id"),
        institutionId = getString("institution_id"),
        moderatorId = getUUID("moderator_id"),
        deletedAt = getLocalDateTime("deleted_at"),
        form = serializer.unserialize(getString("data"), DynamicForm::class.java),
        createdAt = getLocalDateTime("created_at"),
        images = getArrayOfStrings("images").toList()
    )
}
