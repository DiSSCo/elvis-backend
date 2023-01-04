package org.synthesis.country

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select

interface CountryFinder {
    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(code: CountryCode): Country?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun findByShortTitle(shortTitle: String): Country?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun findByFullTitle(fullTitle: String): Country?
}

class DefaultCountryFinder(
    private val sqlClient: SqlClient
) : CountryFinder {

    override suspend fun find(code: CountryCode): Country? = sqlClient.fetchOne(
        select("countries") {
            where {
                "iso" eq code.id.uppercase()
            }
        }
    )?.hydrate()

    override suspend fun findByShortTitle(shortTitle: String): Country? = sqlClient.fetchOne(
        select("countries") {
            where {
                "short_name" eq shortTitle.uppercase()
            }
        }
    )?.hydrate()

    override suspend fun findByFullTitle(fullTitle: String): Country? = sqlClient.fetchOne(
        select("countries") {
            where {
                "full_name" eq fullTitle
            }
        }
    )?.hydrate()

    private fun Row.hydrate() = Country(
        isoCode = CountryCode(getString("iso").uppercase()),
        isoFullCode = getString("iso3").uppercase(),
        shortName = getString("short_name"),
        fullName = getString("full_name"),
        currencyCode = getInteger("currency_code"),
        phoneCode = getInteger("phone_code")
    )
}
