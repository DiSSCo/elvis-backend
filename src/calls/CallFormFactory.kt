@file:Suppress("unused")

package org.synthesis.calls

import org.synthesis.formbuilder.*
import org.synthesis.formbuilder.FieldWithValue

/**
 * @todo: DSL
 */

object VaCallFormFactory {

    private enum class FormType {
        GENERAL,
        INSTITUTION
    }

    private val fieldSet: Map<FormType, List<FieldWithValue>> = mapOf(
        FormType.GENERAL to listOf(
            text("subject"),
            text("abstract"),
            text("people_involved"),
            text("impact_1"),
            text("impact_2"),
            text("impact_3"),
            text("impact_4"),
            text("impact_5")
        ),
        FormType.INSTITUTION to listOf(
            text("collections[0].name"),
            text("collections[0].specimens_amount"),
            text("collections[0].preservation_type"),
            text("further_details"),
            text("metadata"),
            text("facilities[0].titles"),
            text("facilities[0].workflow"),
            text("data_storage[0].platform_name"),
            text("data_storage[0].details"),
            text("data_storage[0].cms"),
            checkBox("data_storage[0].formats_csv"),
            checkBox("data_storage[0].formats_jpeg"),
            checkBox("data_storage[0].formats_png"),
            checkBox("data_storage[0].formats_tiff"),
            checkBox("data_storage[0].formats_txt"),
            checkBox("data_storage[0].formats_xlxs"),
            checkBox("data_storage[0].formats_zip"),
            text("data_storage[0].other_format"),
            text("data_storage[0].info"),
            text("data_storage[0].additional_info"),
            text("license"),
            text("license_timeline")
        )
    )

    fun general(): List<FieldWithValue> = fieldSet[FormType.GENERAL] ?: listOf()

    fun institution(): List<FieldWithValue> = fieldSet[FormType.INSTITUTION] ?: listOf()
}

object TaCallFormFactory {

    private enum class FormType {
        GENERAL,
        INSTITUTIONS,
        SCORE
    }

    private val fieldSet: Map<FormType, List<FieldWithValue>> = mapOf(
        FormType.GENERAL to listOf(
            text("request_type"),
            text("subject"),
            text("country_of_visit"),
            text("sup_name"),
            text("sup_institution"),
            text("sup_position"),
            text("sup_email"),
            text("sup_phone"),
            text("sup_statement"),
            text("visit_length"),
            text("start_date"),
            text("end_date"),
            text("team_leader"),
            list("other_researchers"),
            text("project_objectives"),
            text("project_discipline"),
            text("project_specific_discipline"),
            text("project_summary"),
            list("other_institutions"),
            text("why_other_institutions"),
            checkBox("visit_before"),
            list("visit_before_institution"),
            text("visit_before_request_number"),
            text("context_methods"),
            text("work_plan"),
            text("justification_for_request"),
            text("advance_research"),
            text("expected_output"),
            text("training_requirements"),
            text("training_received"),
            checkBox("first_application"),
            text("previous_project_acronyms"),
            checkBox("first_visit_to_selected"),
            text("previous_visit_details"),
            checkBox("facilities_home"),
            text("access_explanation"),
            text("days_in_selected"),
            text("collection"),
            text("libraries_in_selected")
        ),
        FormType.INSTITUTIONS to listOf(
            text("institution"),
            text("host"),
            text("facility[0].service[0]")
            ),
        FormType.SCORE to listOf(
            text("methodology"),
            text("research_excellence"),
            text("support_statement"),
            text("justification"),
            text("gains_outputs"),
            text("societal_challenge"),
            text("merit"),
            text("comments"),
        )
    )

    fun general(): List<FieldWithValue> = fieldSet[FormType.GENERAL] ?: listOf()

    fun score(): List<FieldWithValue> = fieldSet[FormType.SCORE] ?: listOf()

    fun institution(): List<FieldWithValue> = fieldSet[FormType.INSTITUTIONS] ?: listOf()
}
