@file:Suppress("MatchingDeclarationName")

package org.synthesis.account.registration

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size


data class RegistrationRequest(
    @get:NotBlank
    @get:Size(max = 100)
    val firstName: String,
    @get:NotBlank
    @get:Size(min = 2, max = 100)
    val lastName: String,
    @get:NotBlank
    @get:Size(min = 5, max = 20)
    val password: String,
    @get:NotBlank
    @get:Email
    val email: String,
    val orcId: String?,
    val gender: String?,
    val relatedInstitutionId: String?,
    val birthDate: String?,
    val institutionId: String?,
    val nationality: String?,
    val homeInstitutionId: String?,
    val countryOtherInstitution: String?
)

data class UpdateProfileRequest(
    @get:NotBlank
    @get:Size(max = 100)
    val firstName: String,
    @get:NotBlank
    @get:Size(min = 2, max = 100)
    val lastName: String,
    val orcId: String?,
    val gender: String?,
    val relatedInstitutionId: String?,
    val birthDate: String?,
    val nationality: String?,
    val homeInstitutionId: String?,
    val countryOtherInstitution: String?
)
