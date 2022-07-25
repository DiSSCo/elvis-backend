package org.synthesis.institution

import org.synthesis.country.Country
import org.synthesis.country.CountryCode
import org.synthesis.country.CountryFinder

class InstitutionCountryAllocator(
    private val institutionStore: InstitutionStore,
    private val countryFinder: CountryFinder
) {
    suspend fun allocate(id: InstitutionId): Country? = institutionStore
        .findById(id)
        ?.let {
            val countryData = it.country() ?: return@let null

            return@let if (countryData.length == 2) {
                countryFinder.find(CountryCode(countryData))
            } else {
                countryFinder.findByFullTitle(countryData) ?: countryFinder.findByShortTitle(countryData)
            }
        }
}
