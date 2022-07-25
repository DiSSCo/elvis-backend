package org.synthesis.institution

import org.synthesis.infrastructure.ApplicationException
import org.synthesis.institution.facility.FacilityId

sealed class InstituteException(message: String) : ApplicationException(message) {
    class IncorrectGRID : InstituteException("Incorrect GRID value")
    class InstitutionAlreadyAdded(id: InstitutionId) :
        InstituteException("Institute with GRID `${id.grid.value}` already added")

    class InstitutionNotFound(id: InstitutionId) :
        InstituteException("Institution with id `${id.grid.value}` not found")

    class FacilityNotFound(id: FacilityId) : InstituteException("Facility with id `${id.uuid}` was not found")
}
