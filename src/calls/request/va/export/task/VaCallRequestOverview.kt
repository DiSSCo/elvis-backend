package org.synthesis.calls.request.va.export.task

import org.synthesis.calls.request.va.VaCallRequest
import org.synthesis.formbuilder.FieldId
import org.synthesis.formbuilder.FieldValue
import org.synthesis.formbuilder.FieldWithValue
import org.synthesis.institution.InstituteException
import org.synthesis.institution.InstitutionId
import org.synthesis.institution.InstitutionStore

class VaCallRequestOverview(
    private val institutionStore: InstitutionStore
) : ExportTask {
    /**
     * @throws [InstituteException.InstitutionNotFound]
     */
    override suspend fun generate(callRequest: VaCallRequest) = render(
        "va_template.html", mapOf(
            // @todo: request type
            "request.type" to "VA request",
            "request.subject" to callRequest.title(),
            "request.abstract" to callRequest.description(),
            "request.institution_forms" to institutionForms(callRequest.content().institutions),
            "request.impact" to impact(callRequest)
        )
    )

    /**
     * Render institution template
     *
     * @throws [InstituteException.InstitutionNotFound]
     */
    private suspend fun institutionForms(forms: Map<InstitutionId, VaCallRequest.InstitutionForm>): String {
        var institutionIndex = 1

        return forms.map { (id, form) ->
            val institution = institutionStore.findById(id) ?: throw InstituteException.InstitutionNotFound(id)

            render(
                "va_institution_form.html", mapOf(
                    "request.institution_form.title" to institution.title(),
                    "request.institution_form.index" to institutionIndex++.toString(),
                    "request.institution_form.collections" to collections(form),
                    "request.institution_form.collections.further_details" to form.extractValue("further_details"),
                    "request.institution_form.facilities" to facilities(form),
                    "request.institution_form.facilities_metadata" to form.extractValue("metadata"),
                    "request.institution_form.data_storage" to dataStorage(form),
                    "request.institution_form.license" to form.extractValue("license"),
                    "request.institution_form.timeline" to form.extractValue("license_timeline")
                )
            )
        }.joinToString("\r\n")
    }

    /**
     * Render institution facilities info
     */
    private fun facilities(form: VaCallRequest.InstitutionForm) = form.facilities().map { (_, info) ->
        render(
            "va_institution_form_facility.html", mapOf(
                "request.institution_form.facility.required" to info.requiredFacilities,
                "request.institution_form.facility.workflow" to info.proposedWorkflow
            )
        )
    }.joinToString("<br/>")

    /**
     * Render involved collections info
     */
    private fun collections(form: VaCallRequest.InstitutionForm) = form.collections().map { (_, info) ->
        render(
            "va_institution_form_collection.html", mapOf(
                "request.institution_form.collection.name" to info.name,
                "request.institution_form.collection.specimens_number" to info.specimensNumber,
                "request.institution_form.collection.preservation_type" to info.preservationType
            )
        )
    }.joinToString("<br/>")

    /**
     * Render data storage and release section
     */
    private fun dataStorage(form: VaCallRequest.InstitutionForm) = form.dataStorage().map { (_, info) ->
        render(
            "va_institution_form_data_storage.html", mapOf(
                "request.institution_form.data_storage.platform" to info.platform,
                "request.institution_form.data_storage.details" to info.furtherDetails,
                "request.institution_form.data_storage.systems" to info.dataStorageSystems,
                "request.institution_form.data_storage.file_formats" to info.fileFormats.joinToString(", "),
                "request.institution_form.data_storage.other_format" to info.otherFormats,
                "request.institution_form.data_storage.storage_types" to info.otherFormats,
                "request.institution_form.data_storage.additional_info" to info.additionalInfo
            )
        )
    }.joinToString("<br/>")

    /**
     * Render answers for impact questions
     */
    private fun impact(callRequest: VaCallRequest) = render(
        "va_impact.html",
        callRequest
            .impactQuestions()
            .map { (index, answer) -> "request.impact.answer_$index" to answer.toString() }
            .toMap()
    )

    private fun render(name: String, withParams: Map<String, String?>) = loadResource(name).replace(withParams)
    private fun loadResource(name: String): String {
        val path = "/callrequest/export/overview/$name"

        try {
            return this::class.java.getResource(path).readText()
        } catch (e: Throwable) {
            throw Exception("Unable to load resource: `$path`")
        }
    }

    private fun String.replace(replacements: Map<String, String?>): String {
        var content = this

        replacements.map {
            content = content.replace("{${it.key}}", it.value ?: "")
        }

        return content
    }
}

private fun VaCallRequest.impactQuestions() = content().general.values
    .filter { it.key.id.contains("impact_") }
    .map { (fieldId, fieldWithValue) ->
        fieldId.id.split("_").last().toInt() to fieldWithValue.renderValue()
    }

/**
 * Extract specified field value from institution form
 */
private fun VaCallRequest.InstitutionForm.extractValue(field: String): String? {
    val fieldValue = content()
        .values[FieldId.fromString(field)]
        ?.value as? FieldValue.Text
        ?: return null

    return fieldValue.value
}

/**
 * Collect institution form facilities information
 */
private fun VaCallRequest.InstitutionForm.facilities(): Map<Int, InstitutionFacility> {
    val facilities: MutableMap<Int, InstitutionFacility> = mutableMapOf()

    content().values
        .filter { it.key.groupId?.id == "facilities" }
        .map { (fieldId, fieldWithValue) ->

            val position = fieldId.position()
            val renderedValue = fieldWithValue.renderValue().toString()

            if (!facilities.containsKey(position)) facilities[fieldId.position()] = InstitutionFacility()

            @Suppress("UnsafeCallOnNullableType")
            when (fieldWithValue.field.id) {
                "titles" -> facilities[position]!!.requiredFacilities = renderedValue
                "workflow" -> facilities[position]!!.proposedWorkflow = renderedValue
            }
        }

    return facilities
}

/**
 * Collect institution form involved collections
 */
private fun VaCallRequest.InstitutionForm.collections(): Map<Int, InstitutionCollection> {

    val collections: MutableMap<Int, InstitutionCollection> = mutableMapOf()

    content().values
        .filter { it.key.groupId?.id == "collections" }
        .map { (fieldId, fieldWithValue) ->

            val position = fieldId.position()
            val renderedValue = fieldWithValue.renderValue().toString()

            if (!collections.containsKey(position)) collections[fieldId.position()] = InstitutionCollection()

            @Suppress("UnsafeCallOnNullableType")
            when (fieldWithValue.field.id) {
                "name" -> collections[position]!!.name = renderedValue
                "specimens_amount" -> collections[position]!!.specimensNumber = renderedValue
                "preservation_type" -> collections[position]!!.preservationType = renderedValue
            }
        }

    return collections
}

@Suppress("ComplexMethod")
private fun VaCallRequest.InstitutionForm.dataStorage(): Map<Int, InstitutionDataStorage> {
    val collections: MutableMap<Int, InstitutionDataStorage> = mutableMapOf()

    content().values
        .filter { it.key.groupId?.id == "data_storage" }
        .map { (fieldId, fieldWithValue) ->

            val position = fieldId.position()
            val renderedValue = fieldWithValue.renderValue().toString()

            if (!collections.containsKey(position)) collections[fieldId.position()] = InstitutionDataStorage()

            @Suppress("UnsafeCallOnNullableType")
            when (fieldWithValue.field.id) {
                "platform_name" -> collections[position]!!.platform = renderedValue
                "details" -> collections[position]!!.furtherDetails = renderedValue
                "cms" -> collections[position]!!.dataStorageSystems = renderedValue
                "other_format" -> collections[position]!!.otherFormats = renderedValue
                "info" -> collections[position]!!.additionalInfo = renderedValue
                else -> if (fieldWithValue.field.id.contains("formats_")) {
                    if (fieldWithValue.value is FieldValue.Checkbox && fieldWithValue.value.value) {
                        collections[position]!!.fileFormats.add(fieldId.id.split("_").last())
                    }
                } else Unit
            }
        }

    return collections
}

private fun FieldId.position() = groupId?.position ?: throw Exception("Unexpected call")

/**
 * Render value as text representation
 */
private fun FieldWithValue.renderValue(): Any = when (value) {
    is FieldValue.Text -> value.value
    is FieldValue.Checkbox -> if (value.value) "yes" else "no"
    is FieldValue.List -> value.value
    null -> "t&#8212;"
}

private data class InstitutionFacility(
    var requiredFacilities: String? = null,
    var proposedWorkflow: String? = null
)

private data class InstitutionCollection(
    var name: String? = null,
    var specimensNumber: String? = null,
    var preservationType: String? = null
)

private data class InstitutionDataStorage(
    var platform: String? = null,
    var furtherDetails: String? = null,
    var dataStorageSystems: String? = null,
    var fileFormats: MutableList<String> = mutableListOf(),
    var otherFormats: String? = null,
    var storageSystemInfo: String? = null,
    var additionalInfo: String? = null
)
