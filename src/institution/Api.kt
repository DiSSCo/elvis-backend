@file:Suppress("unused")

package org.synthesis.institution

import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size
import org.synthesis.account.UserAccountId
import org.synthesis.country.CountryCode
import org.synthesis.formbuilder.FieldValue
import org.synthesis.institution.coordinator.CoordinatorType

sealed class InstitutionCommand {
    data class Add(
        @get:NotBlank
        @get:Size(min = 5, max = 20)
        val id: String,
        @get:NotBlank
        val name: String,
        @get:NotBlank
        val cetaf: String
    ) : InstitutionCommand()

    data class Sync(
        @get:NotBlank
        @get:Size(min = 5, max = 20)
        val id: String
    ) : InstitutionCommand()

    data class Update(
        val fieldId: String,
        val fieldValue: FieldValue
    ) : InstitutionCommand()

    data class ChangeName(
        val name: String,
        val cetaf: String
    ) : InstitutionCommand()

    data class SetHightlightAddress(
        val fields: List<String>
    ) : InstitutionCommand()

    data class RemoveGroup(
        val groupId: String
    ) : InstitutionCommand()
}

data class InstitutionResponse(
    val id: InstitutionId,
    val name: String,
    val cetaf: CETAF,
    val fieldValues: Map<String, Any?>,
    val coordinators: List<CoordinatorData>
)

data class VACoordinatorApiResponse(
    val id: UUID,
    val institutionId: InstitutionId,
    val email: String,
    val firstName: String?,
    val lastName: String?
)

data class CoordinatorData(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val institutionId: InstitutionId,
    val type: CoordinatorType
)

data class ScorerData(
    val id: UserAccountId,
    val email: String,
    val firstName: String,
    val lastName: String,
    val country: CountryCode
)
data class TACoordinatorData(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val institutionId: InstitutionId
)
data class SetFacilityDataRequest(
    val fieldId: String,
    val fieldValue: FieldValue
)

data class FacilityFieldValueResponse(
    val type: String,
    val value: Any?
)

data class FacilityResponse(
    val id: UUID,
    val institutionName: String,
    val institutionId: InstitutionId,
    val fieldValues: Map<String, Any?>,
    val images: List<String>
)

data class RemoveFieldGroupRequest(
    val groupId: String
)

data class ManageInstituteMember(
    val id: UUID
)
