package org.synthesis.calls.store

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import org.synthesis.calls.*
import org.synthesis.calls.request.CallRequestId
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select

interface CallFinder {
    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(id: CallId): Call?

    /**
     *
     *
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(id: CallRequestId): Call?
}

class PostgresCallFinder(
    private val sqlClient: SqlClient
) : CallFinder {

    override suspend fun find(id: CallId): Call? {
        return sqlClient.fetchOne(
            select("calls") {
                where { "id" eq id.uuid }
            }
        )?.hydrate()
    }

    override suspend fun find(id: CallRequestId): Call? {
        val vaCallRow = sqlClient.fetchOne(
            select("requests") {
                where { "id" eq id.uuid }
            }
        )

        return vaCallRow?.let {
            find(CallId(it.getUUID("call_id")))
        }
    }

    private fun Row.hydrate(): Call = Call(
        id = CallId(getUUID("id")),
        type = CallType.valueOf(getString("call_type").toUpperCase()),
        lifetime = CallLifeTime.create(
            from = getLocalDateTime("start_date"),
            to = getLocalDateTime("end_date")
        ),
        info = CallInfo(
            name = getString("title"),
            description = getString("description"),
            additionalInformation = getString("additional_info"),
            scoring = ScoringData(
                endDate = getLocalDateTime("scoring_end_date"),
                weight = ScoringWeight(
                    methodology = getString("methodology"),
                    researchExcellence = getString("research_excellence"),
                    supportStatement = getString("support_statement"),
                    justification = getString("justification"),
                    gainsOutputs = getString("gains_outputs"),
                    societalChallenge = getString("societal_challenge"),
                    merit = getString("merit"),
                )
            )
        ),
        deletedAt = getLocalDateTime("deleted_at")
    )
}
