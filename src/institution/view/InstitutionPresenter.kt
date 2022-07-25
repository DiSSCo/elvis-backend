package org.synthesis.institution.view

import kotlinx.coroutines.flow.toList
import org.synthesis.formbuilder.normalize
import org.synthesis.institution.*
import org.synthesis.institution.coordinator.CoordinatorAllocator

class InstitutionPresenter(
    private val institutionStore: InstitutionStore,
    private val coordinatorAllocator: CoordinatorAllocator
) {

    suspend fun find(id: InstitutionId): InstitutionResponse? {
        val institution = institutionStore.findById(id) ?: return null

        return institution.transform()
    }

    suspend fun findAll(): List<InstitutionResponse?> {
        return institutionStore.findByAll().map { it?.transform() }
    }

    private suspend fun Institution.transform(): InstitutionResponse = InstitutionResponse(
        id = id(),
        fieldValues = content().normalize(),
        cetaf = cetaf(),
        name = title(),
        coordinators = coordinatorAllocator.all(id()).toList(),
    )
}
