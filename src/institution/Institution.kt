package org.synthesis.institution

import com.fasterxml.jackson.annotation.JsonValue
import org.synthesis.country.CountryCode
import org.synthesis.formbuilder.*

data class CETAF(
    @JsonValue
    val value: String
)

data class GRID(
    @JsonValue
    val value: String
)

data class InstitutionId(
    @JsonValue
    val grid: GRID
) {
    companion object Factory {

        fun fromString(gridValue: String?): InstitutionId {
            if (gridValue.isNullOrBlank()) {
                throw InstituteException.IncorrectGRID()
            }

            return InstitutionId(GRID(gridValue))
        }
    }

    override fun toString(): String = grid.value
}

object InstitutionAddressFactory {
    private enum class FormType {
        GENERAL
    }

    private val fieldSet: Map<FormType, List<FieldWithValue>> = mapOf(
        FormType.GENERAL to listOf(
            text("locationName"),
            text("name"),
            text("locationType"),
            text("street"),
            text("number"),
            text("coordinating_taf"),
            text("city"),
            text("zipCode"),
            text("contactPerson"),
            text("contactPersonMail"),
            text("description"),
            list("country"),
            text("aff_address[0].name"),
            text("aff_address[0].type"),
            text("aff_address[0].street"),
            text("aff_address[0].number"),
            text("aff_address[0].zipcode"),
            text("aff_address[0].city"),
            text("aff_address[0].contact_person"),
            text("aff_address[0].contact_person_email"),
        )
    )

    fun general(): List<FieldWithValue> = fieldSet[FormType.GENERAL] ?: listOf()
}

class Institution(
    private val id: InstitutionId,
    private var name: String,
    private var cetaf: CETAF,
    private val form: DynamicForm,
    private val countryCode: CountryCode? = null
) {
    fun id(): InstitutionId = id
    fun title(): String = name
    fun cetaf(): CETAF = cetaf
    fun content(): DynamicForm = form

    fun updateCetaf(new: CETAF) {
        cetaf = new
    }

    /**
     * Rename institution.
     */
    fun rename(new: String) {
        name = new
    }

    /**
     * @throws [FormBuilderExceptions.FieldNotFound]
     * @throws [FormBuilderExceptions.FieldTypeMismatch]
     */
    fun setFieldValue(fieldId: FieldId, value: FieldValue?) {
        form.setFieldValue(fieldId, value)
    }

    fun removeGroup(id: GroupId) {
        form.deleteGroup(id)
    }

    /**
     * Returns the country the institute is bound to.
     * Can return both ISO country code and country name (for backward compatibility)
     */
    fun country(): String? {
        val fieldValue = form.extractFieldValue(FieldId.fromString("country")) ?: return countryCode?.id

        if (fieldValue.value !is FieldValue.List) {
            return null
        }

        /** For some reason, a list is stored here, but formally the value can only be one. */
        return fieldValue.value.value.first()
    }
}
