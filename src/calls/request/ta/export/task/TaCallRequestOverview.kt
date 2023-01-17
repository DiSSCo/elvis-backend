package org.synthesis.calls.request.ta.export.task

import org.synthesis.calls.request.ta.TaCallRequest
import org.synthesis.formbuilder.*
import org.synthesis.institution.InstituteException
import org.synthesis.institution.InstitutionId
import org.synthesis.institution.InstitutionStore
import org.synthesis.institution.facility.Facility
import org.synthesis.institution.facility.FacilityStore

class TaCallRequestOverview(
    private val institutionStore: InstitutionStore,
    private val facilityStore: FacilityStore
) : ExportTask {
    /**
     * @throws [InstituteException.InstitutionNotFound]
     */
    override suspend fun generate(callRequest: TaCallRequest) = render(
        "ta_template.html", mapOf(
            "request.type" to "Ta request",
            "request.subject" to callRequest.extractValue("subject"),
            "request.institutionName" to institutionNames(callRequest.content().institutions),
            "request.start_date" to callRequest.extractValue("start_date"),
            "request.end_date" to callRequest.extractValue("end_date"),
            "request.visit_length" to callRequest.extractValue("visit_length"),
            "request.team_leader" to callRequest.extractValue("team_leader"),
            "request.other_researchers" to callRequest.extractValue("other_researchers"),
            "request.project_objectives" to callRequest.extractValue("project_objectives"),
            "request.visit_before" to callRequest.extractValue("visit_before"),
            "request.project_discipline" to callRequest.extractValue("project_discipline"),
            "request.project_specific_discipline" to callRequest.extractValue("project_specific_discipline"),
            "request.project_summary" to callRequest.extractValue("project_summary"),
            "request.other_institutions" to callRequest.extractValue("other_institutions"),
            "request.why_other_institutions" to callRequest.extractValue("why_other_institutions"),
            "request.visit_before" to callRequest.extractValue("visit_before"),
            "request.visit_before_institution" to callRequest.extractValue("visit_before_institution"),
            "request.visit_before_request_number" to callRequest.extractValue("visit_before_request_number"),
            "request.context_methods" to callRequest.extractValue("context_methods"),
            "request.work_plan" to callRequest.extractValue("work_plan"),
            "request.justification_for_request" to callRequest.extractValue("justification_for_request"),
            "request.advance_research" to callRequest.extractValue("advance_research"),
            "request.expected_output" to callRequest.extractValue("expected_output"),
            "request.training_requirements" to callRequest.extractValue("training_requirements"),
            "request.training_received" to callRequest.extractValue("training_received"),
            "request.first_application" to callRequest.extractValue("first_application"),
            "request.previous_project_acronyms" to callRequest.extractValue("previous_project_acronyms"),
            "request.first_visit_to_selected" to callRequest.extractValue("first_visit_to_selected"),
            "request.previous_visit_details" to callRequest.extractValue("previous_visit_details"),
            "request.facilities_home" to callRequest.extractValue("facilities_home"),
            "request.access_explanation" to callRequest.extractValue("access_explanation"),
            "request.days_in_selected" to callRequest.extractValue("days_in_selected"),
            "request.libraries_in_selected" to callRequest.extractValue("libraries_in_selected"),
            "request.collection" to callRequest.extractValue("collection"),
            "request.sup_name" to callRequest.extractValue("sup_name"),
            "request.sup_institution" to callRequest.extractValue("sup_institution"),
            "request.sup_position" to callRequest.extractValue("sup_position"),
            "request.sup_email" to callRequest.extractValue("sup_email"),
            "request.sup_phone" to callRequest.extractValue("sup_phone"),
            "request.sup_statement" to callRequest.extractValue("sup_statement"),
            "request.institutionServices" to institutionFacility(callRequest.content().institutions),
        )
    )

    /**
     * Render institution template
     *
     * @throws [InstituteException.InstitutionNotFound]
     */
    private suspend fun institutionNames(forms: Map<InstitutionId, TaCallRequest.InstitutionForm>): String {
        var institutionIndex = 1

        return forms.map { (id) ->
            val institution = institutionStore.findById(id) ?: throw InstituteException.InstitutionNotFound(id)
            render(
                "ta_institution_name.html", mapOf(
                    "request.institution_form.title" to institution.title(),
                    "request.institution_form.host" to forms.getValue(id).extractValue("host"),
                    "request.institution_form.index" to institutionIndex++.toString()
                )
            )
        }.joinToString("\r\n")
    }

    private suspend fun institutionFacility(forms: Map<InstitutionId, TaCallRequest.InstitutionForm>): String {
        var institutionIndex = 1
        return forms.map { (id, form) ->
            val institution = institutionStore.findById(id) ?: throw InstituteException.InstitutionNotFound(id)
            val facility = facilityStore.loadByInstitutionId(id)
            render(
                "ta_institution_services.html", mapOf(
                    "request.institution_services.index" to institutionIndex++.toString(),
                    "request.institution_services.data" to getService(form.content(), facility),
                    "request.institution_services.title" to institution.title()
                )
            )
        }.joinToString("\r\n")
    }

    private fun getService(form: DynamicForm, facilities: List<Facility?>): String {
        return form.values.filter { it.value.field.id == "service" }.map {
            val facility = facilities[it.key.groupId?.position!!]
            val days = it.value.clearValue().toString()
            val serviceName = facility?.formValue(
                FieldId(
                    "name",
                    GroupId("instruments", it.key.position)
                )
            )
            render(
                "ta_institution_services_data.html", mapOf(
                    "request.institution_services.title" to facility?.title(),
                    "request.institution_services.name" to serviceName,
                    "request.institution_services.days" to days
                )
            )
        }.joinToString("\r\n")
    }

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

private fun FieldId.position() = groupId?.position ?: throw Exception("Unexpected call")

/**
 * Extract specified field value from institution form
 */
private fun TaCallRequest.InstitutionForm.extractValue(field: String): String? {
    val fieldValue = content()
        .values[FieldId.fromString(field)]
        ?.value as? FieldValue.Text
        ?: return null

    return fieldValue.value
}

/**
 * Extract specified field value from institution form
 */
private fun TaCallRequest.extractValue(field: String): String? {

    val fieldType = content().general
        .values[FieldId.fromString(field)]?.type?.name ?: return null

    when (fieldType) {
        "List" -> {
            val fieldValue = content().general
                .values[FieldId.fromString(field)]?.value as? FieldValue.List
                ?: return null
            return fieldValue.value.joinToString(", ")
        }
        "Boolean" -> {
            val fieldValue = content().general
                .values[FieldId.fromString(field)]?.value as? FieldValue.Checkbox
                ?: return null
            when (fieldValue.value) {
                true -> return fieldValue.value.toString().replace("true", "Yes")
                false -> return fieldValue.value.toString().replace("false", "No")
                else -> return null
            }
        }
        else -> {
            val fieldValue = content().general
                .values[FieldId.fromString(field)]?.value as? FieldValue.Text
                ?: return null
            return fieldValue.value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
        }
    }
}
