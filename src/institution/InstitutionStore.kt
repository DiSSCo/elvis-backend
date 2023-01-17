package org.synthesis.institution

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.synthesis.country.Country
import org.synthesis.country.CountryCode
import org.synthesis.country.CountryFinder
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.infrastructure.persistence.*
import org.synthesis.infrastructure.persistence.querybuilder.*
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.infrastructure.serializer.Serializer

interface InstitutionStore {

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun findById(id: InstitutionId): Institution?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun findByAll(): List<Institution?>

    /**
     * @throws [StorageException.InteractingFailed]
     * @throws [StorageException.UniqueConstraintViolationCheckFailed]
     */
    suspend fun add(institution: Institution)

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun save(institution: Institution)
}

class PostgresInstitutionStore(
    private val sqlClient: SqlClient,
    private val serializer: Serializer = JacksonSerializer,
    private val countryFinder: CountryFinder
) : InstitutionStore {

    override suspend fun findById(id: InstitutionId): Institution? = sqlClient.fetchOne(
        select("institutions") {
            where { "id" eq id.grid.value }
        }
    )?.hydrate()

    override suspend fun findByAll(): List<Institution?> = sqlClient.fetchAll(
        select("institutions")
    ).map { it.hydrate() }.toList()

    override suspend fun add(institution: Institution) {
        sqlClient.execute(
            insert(
                "institutions", mapOf(
                    "id" to institution.id().grid.value,
                    "cetaf" to institution.cetaf().value,
                    "title" to institution.title(),
                    "country_code" to institution.prepareCountry()?.isoCode?.id?.uppercase(),
                    "data" to serializer.serialize(institution.content())
                )
            )
        )
    }

    override suspend fun save(institution: Institution) {
        sqlClient.execute(
            update(
                "institutions", mapOf(
                    "data" to serializer.serialize(institution.content()),
                    "title" to institution.title(),
                    "cetaf" to institution.cetaf().value,
                    "country_code" to institution.prepareCountry()?.isoCode?.id?.uppercase(),
                )
            ) {
                where { "id" eq institution.id().grid.value }
            }
        )
    }

    private suspend fun Institution.prepareCountry(): Country? {
        val countryData = country() ?: return null

        return if (countryData.length == 2) {
            countryFinder.find(CountryCode(countryData))
        } else {
            countryFinder.findByFullTitle(countryData) ?: countryFinder.findByShortTitle(countryData)
        }
    }

    private fun Row.hydrate() = Institution(
        id = InstitutionId(GRID(getString("id"))),
        name = getString("title"),
        cetaf = CETAF(getString("cetaf")),
        form = serializer.unserialize(getString("data"), DynamicForm::class.java),
        countryCode = getString("country_code")?.let { CountryCode(it.uppercase()) }
    )
}
