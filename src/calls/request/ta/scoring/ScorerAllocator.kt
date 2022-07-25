package org.synthesis.calls.request.ta.scoring

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import org.synthesis.account.UserAccountId
import org.synthesis.country.CountryCode
import org.synthesis.infrastructure.persistence.*
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select
import org.synthesis.institution.ScorerData

interface ScorerAllocator {

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(id: UserAccountId, countryCode: CountryCode): ScorerData?
}

class PostgresScorerAllocator(
    private val sqlClient: SqlClient
) : ScorerAllocator {

    override suspend fun find(id: UserAccountId, countryCode: CountryCode): ScorerData? = sqlClient.fetchOne(
        select(
            from = "scorers as s",
            columns = listOf(
                "s.user_id as id",
                "s.country_code as country_code",
                "a.email as email",
                "a.first_name as first_name",
                "a.last_name as last_name"
            )
        ) {
            "accounts a" innerJoin "s.user_id = a.id"

            where {
                "s.user_id" eq id.uuid
                "s.country_code" eq countryCode.id.toUpperCase()
            }
        }
    )?.hydrate()

    private fun Row.hydrate(): ScorerData = ScorerData(
        id = UserAccountId(getUUID("id")),
        email = getString("email"),
        firstName = getString("first_name"),
        lastName = getString("last_name"),
        country = CountryCode(getString("country_code"))
    )
}
