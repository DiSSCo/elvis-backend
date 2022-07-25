package org.synthesis.country

import com.fasterxml.jackson.annotation.JsonValue

data class CountryCode(
    @JsonValue
    val id: String
)

data class Country(
    val isoCode: CountryCode,
    val isoFullCode: String,
    val shortName: String,
    val fullName: String,
    val currencyCode: Int,
    val phoneCode: Int
)
