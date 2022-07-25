package org.synthesis.calls.request.ta.presenter

import org.synthesis.account.UserAccountFinder
import org.synthesis.account.UserAccountId
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.ta.TaCallRequest
import org.synthesis.calls.request.ta.store.TaCallRequestFinder
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.formbuilder.FieldWithValue
import org.synthesis.institution.InstitutionStore

class TaCallRequestPresenter(
    private val taCallRequestFinder: TaCallRequestFinder,
    private val institutionStore: InstitutionStore,
    private val userAccountFinder: UserAccountFinder
) {
    suspend fun find(id: CallRequestId): TaCallRequestResponse? = taCallRequestFinder.find(id)?.mapToDetails()

    private suspend fun TaCallRequest.mapToDetails(): TaCallRequestResponse {
        val institutions = content().institutions.values.map {
            TaCallRequestResponse.Institution(
                id = it.institutionId().grid.value,
                name = institutionStore.findById(it.institutionId())?.title() ?: "",
                status = it.status().name.toLowerCase(),
                fieldValues = it.content().mapToFieldValues()
            )
        }
        val scoring = content().score.values.map {
            TaCallRequestResponse.Scoring(
            id = it.id()
            )
        }

        return TaCallRequestResponse(
            id = id().uuid,
            callId = callId().uuid,
            status = status().name.toLowerCase(),
            fieldValues = content().general.mapToFieldValues(),
            institutions = institutions,
            scoreFormId = scoring,
            creatorData = userAccountFinder.find(UserAccountId(authorId().uuid))?.asTaCallRequesterData()
        )
    }

    private fun DynamicForm.mapToFieldValues() = values
        .values
        .map { it.mapToFieldValue() }

    private fun FieldWithValue.mapToFieldValue() = TaCallRequestResponse.FieldValue(
        fieldId = field.toString(),
        value = value
    )
}
