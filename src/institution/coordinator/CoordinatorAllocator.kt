package org.synthesis.institution.coordinator

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.synthesis.account.UserAccountId
import org.synthesis.infrastructure.persistence.*
import org.synthesis.infrastructure.persistence.querybuilder.fetchAll
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select
import org.synthesis.institution.CoordinatorData
import org.synthesis.institution.InstitutionId

interface CoordinatorAllocator {

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(id: UserAccountId): CoordinatorData?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun allocate(institutionId: InstitutionId, accessType: CoordinatorType): CoordinatorData?

    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun all(institutionId: InstitutionId): Flow<CoordinatorData>
}

class PostgresCoordinatorAllocator(
    private val sqlClient: SqlClient
) : CoordinatorAllocator {

    override suspend fun find(id: UserAccountId): CoordinatorData? = sqlClient.fetchOne(
        select(
            from = "institutions_coordinators ic",
            columns = listOf(
                "ic.user_id as user_id",
                "ic.institution_id as institution_id",
                "ic.access as access",
                "a.email as email",
                "a.first_name as first_name",
                "a.last_name as last_name"
            )
        ) {
            "accounts a" innerJoin "ic.user_id = a.id"

            where { "ic.user_id" eq id.uuid }
        }
    )?.hydrate()

    override suspend fun allocate(institutionId: InstitutionId, accessType: CoordinatorType): CoordinatorData? =
        sqlClient.fetchOne(
            select(
                from = "institutions_coordinators ic",
                columns = listOf(
                    "ic.user_id as user_id",
                    "ic.institution_id as institution_id",
                    "ic.access as access",
                    "a.email as email",
                    "a.first_name as first_name",
                    "a.last_name as last_name"
                )
            ) {
                "accounts a" innerJoin "ic.user_id = a.id"
                limit = 1

                where {
                    "ic.institution_id" eq institutionId.grid.value
                    "ic.access" eq accessType.name.toLowerCase()
                }
            }
        )?.hydrate()

    override suspend fun all(institutionId: InstitutionId): Flow<CoordinatorData> = sqlClient.fetchAll(
        select(
            from = "institutions_coordinators ic",
            columns = listOf(
                "ic.user_id as user_id",
                "ic.institution_id as institution_id",
                "ic.access as access",
                "a.email as email",
                "a.first_name as first_name",
                "a.last_name as last_name"
            )
        ) {

            "accounts a" innerJoin "ic.user_id = a.id"

            where { "ic.institution_id" eq institutionId.grid.value }
        }
    ).map { it.hydrate() }

    private fun Row.hydrate() = CoordinatorData(
        id = getUUID("user_id"),
        email = getString("email"),
        firstName = getString("first_name"),
        lastName = getString("last_name"),
        institutionId = InstitutionId.fromString(getString("institution_id")),
        type = CoordinatorType.valueOf(getString("access").toString().uppercase())
    )
}
