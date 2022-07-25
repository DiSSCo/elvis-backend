package org.synthesis.institution.facility

import java.time.LocalDateTime
import org.synthesis.account.UserAccountId
import org.synthesis.attachment.*
import org.synthesis.formbuilder.*
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.institution.InstituteException
import org.synthesis.institution.InstitutionId

sealed class FacilityCommand {

    abstract val id: FacilityId

    data class Create(
        override val id: FacilityId,
        val moderatorId: UserAccountId,
        val institutionId: InstitutionId,
        /** @todo: fix me */
        val formFields: Map<FieldId, Class<out FieldValue>> = mapOf(
            FieldId.fromString("nameEng") to FieldValue.Text::class.java,
            FieldId.fromString("nameLocal") to FieldValue.Text::class.java,
            FieldId.fromString("institution_address") to FieldValue.Text::class.java,
            FieldId.fromString("facilityType") to FieldValue.Text::class.java,
            FieldId.fromString("facilityDescription") to FieldValue.Text::class.java,
            FieldId.fromString("siteUrl") to FieldValue.Text::class.java,
            FieldId.fromString("img") to FieldValue.Text::class.java,
            FieldId.fromString("instruments[0].name") to FieldValue.Text::class.java,
            FieldId.fromString("instruments[0].description") to FieldValue.Text::class.java
        )
    ) : FacilityCommand()

    data class SetField(
        override val id: FacilityId,
        val fieldId: String,
        val fieldValue: FieldValue
    ) : FacilityCommand()

    data class AddImages(
        override val id: FacilityId,
        val storedFiles: List<AttachmentId>
    ) : FacilityCommand()

    data class RemoveImages(
        override val id: FacilityId,
        val storedImage: AttachmentId
    ) : FacilityCommand()

    data class Remove(
        override val id: FacilityId
    ) : FacilityCommand()

    data class RemoveGroup(
        override val id: FacilityId,
        val groupId: String
    ) : FacilityCommand()
}

class FacilityProvider(
    private val store: FacilityStore
) {
    /**
     * @throws [StorageException.InteractingFailed]
     * @throws [StorageException.UniqueConstraintViolationCheckFailed]
     */
    suspend fun handle(command: FacilityCommand.Create) {
        val facility = Facility(
            moderatorId = command.moderatorId,
            id = command.id,
            institutionId = command.institutionId,
            createdAt = LocalDateTime.now(),
            form = DynamicForm(
                command.formFields
                    .map { (fieldId, valueType) ->
                        fieldId to FieldWithValue.withoutValue(fieldId, valueType)
                    }
                    .toMap()
                    .toMutableMap()
            )
        )
        store.add(facility)
    }

    /**
     * @throws [InstituteException.FacilityNotFound]
     * @throws [StorageException.InteractingFailed]
     * @throws [FormBuilderExceptions.FieldNotFound]
     * @throws [FormBuilderExceptions.FieldTypeMismatch]
     * @throws [FormBuilderExceptions.ParseFieldFailed]
     */
    suspend fun handle(command: FacilityCommand.SetField) {
        val facility = store.load(command.id) ?: throw InstituteException.FacilityNotFound(command.id)

        facility.setFieldValue(FieldId.fromString(command.fieldId), command.fieldValue)

        store.save(facility)
    }

    /**
     * @throws [InstituteException.FacilityNotFound]
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun handle(command: FacilityCommand.Remove) {
        val facility = store.load(command.id) ?: throw InstituteException.FacilityNotFound(command.id)

        facility.remove()

        store.save(facility)
    }

    /**
     * @throws [InstituteException.FacilityNotFound]
     * @throws [StorageException.InteractingFailed]
     * @throws [FormBuilderExceptions.ParseFieldFailed]
     */
    suspend fun handle(command: FacilityCommand.RemoveGroup) {
        val facility = store.load(command.id) ?: throw InstituteException.FacilityNotFound(command.id)
        val group = GroupId.fromString(command.groupId)

        facility.removeGroup(group)

        store.save(facility)
    }

    /**
     * @throws [InstituteException.FacilityNotFound]
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun handle(command: FacilityCommand.AddImages) {
        val facility = store.load(command.id) ?: throw InstituteException.FacilityNotFound(command.id)

        command.storedFiles.forEach { facility.addImage(it.uuid.toString()) }

        store.save(facility)
    }

    suspend fun handle(command: FacilityCommand.RemoveImages) {
        val facility = store.load(command.id) ?: throw InstituteException.FacilityNotFound(command.id)
        facility.images().remove(command.storedImage.uuid.toString())
        store.save(facility)
    }
}
