package org.synthesis.institution.facility

import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDateTime
import java.util.*
import org.synthesis.account.UserAccountId
import org.synthesis.formbuilder.*
import org.synthesis.institution.InstitutionId

data class FacilityId(
    @JsonValue
    val uuid: UUID
) {
    companion object {
        fun next(): FacilityId = FacilityId(UUID.randomUUID())
    }
}

class Facility(
    val id: FacilityId,
    val institutionId: InstitutionId,
    val moderatorId: UserAccountId,
    var deletedAt: LocalDateTime? = null,
    var form: DynamicForm,
    val createdAt: LocalDateTime,
    private val images: MutableList<String> = mutableListOf()
) {

    /**
     * @throws [FormBuilderExceptions.FieldNotFound]
     * @throws [FormBuilderExceptions.FieldTypeMismatch]
     */
    fun setFieldValue(fieldId: FieldId, value: FieldValue?) {
        form.setFieldValue(fieldId, value)
    }

    fun images(): MutableList<String> = images

    fun title(): String? = formValue(FieldId.fromString("nameEng"))

    fun localTitle(): String? = formValue(FieldId.fromString("nameLocal"))

    fun remove() {
        deletedAt = LocalDateTime.now()
    }

    fun removeGroup(id: GroupId) {
        form.deleteGroup(id)
    }

    fun addImage(storedFileIdentifier: String) {
        images.add(storedFileIdentifier)
    }

    /**
     * Extract specified field value from form
     */
     fun formValue(field: FieldId): String? {
        val fieldValue = form
            .values[field]
            ?.value as? FieldValue.Text
            ?: return null

        return fieldValue.value
    }
}
