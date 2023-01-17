package org.synthesis.calls.request.va.presenter

import org.synthesis.account.UserAccountFinder
import org.synthesis.account.UserAccountId
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.va.VaCallRequest
import org.synthesis.calls.request.va.store.VaCallRequestFinder
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.formbuilder.FieldWithValue
import org.synthesis.institution.InstitutionStore

class VaCallRequestPresenter(
    private val vaCallRequestFinder: VaCallRequestFinder,
    private val institutionStore: InstitutionStore,
    private val userAccountFinder: UserAccountFinder
) {
    suspend fun find(id: CallRequestId): VaCallRequestResponse? = vaCallRequestFinder.find(id)?.mapToDetails()

    private suspend fun VaCallRequest.mapToDetails(): VaCallRequestResponse {
        val institutions = content().institutions.values.map {
            VaCallRequestResponse.Institution(
                id = it.institutionId().grid.value,
                name = institutionStore.findById(it.institutionId())?.title() ?: "",
                status = it.status().name.lowercase(),
                fieldValues = it.content().mapToFieldValues()
            )
        }

        return VaCallRequestResponse(
            id = id().uuid,
            callId = callId().uuid,
            status = status().name.lowercase(),
            fieldValues = content().general.mapToFieldValues(),
            institutions = institutions,
            creatorData = userAccountFinder.find(UserAccountId(authorId().uuid))?.asVaCallRequesterData()
        )
    }

    private fun DynamicForm.mapToFieldValues() = values
        .values
        .map { it.mapToFieldValue() }

    private fun FieldWithValue.mapToFieldValue() = VaCallRequestResponse.FieldValue(
        fieldId = field.toString(),
        value = value
    )
}
