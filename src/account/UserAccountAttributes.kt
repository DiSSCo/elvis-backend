package org.synthesis.account

import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDate
import org.synthesis.country.CountryCode
import org.synthesis.institution.InstitutionId

data class UserAccountAttributes(
    val gender: Gender,
    val orcId: OrcId? = null,
    val institutionId: InstitutionId? = null,
    val relatedInstitutionId: InstitutionId? = null,
    val birthDate: LocalDate? = null,
    val countryCode: CountryCode? = null,
    val nationality: String? = null,
    val homeInstitutionId: String? = null,
    val countryOtherInstitution: String? = null
)

enum class Gender {
    MALE,
    FEMALE,
    OTHER
}

/**
 * @todo: Validate 0000-0000-0000-0000 pattern
 */
data class OrcId(
    @JsonValue
    val id: String
)
