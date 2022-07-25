package org.synthesis.institution.coordinator

import io.vertx.pgclient.PgPool
import org.synthesis.account.UserAccountId
import org.synthesis.infrastructure.persistence.querybuilder.execute
import org.synthesis.infrastructure.persistence.querybuilder.transaction
import org.synthesis.institution.InstitutionId

interface CoordinatorStore {
    suspend fun update(institutionId: InstitutionId, coordinatorId: UserAccountId, type: String)
}

class PgCoordinatorStore(
    private val sqlClient: PgPool
) : CoordinatorStore {
    override suspend fun update(institutionId: InstitutionId, coordinatorId: UserAccountId, type: String) {
        sqlClient.transaction {
            execute(
                org.synthesis.infrastructure.persistence.querybuilder.update(
                    on = "institutions_coordinators",
                    rows = mapOf(
                        "user_id" to coordinatorId.uuid
                    )
                ) {
                    where {
                        "institution_id" eq institutionId.grid.value
                        "access" eq type
                    }
                }
            )
        }
    }
}
