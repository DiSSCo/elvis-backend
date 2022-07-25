package org.synthesis.calls.store

import io.vertx.sqlclient.SqlClient
import org.synthesis.calls.Call
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.infrastructure.persistence.querybuilder.OnConflict
import org.synthesis.infrastructure.persistence.querybuilder.execute
import org.synthesis.infrastructure.persistence.querybuilder.insert

interface CallStore {
    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun save(call: Call)
}

class PostgresCallStore(
    private val sqlClient: SqlClient
) : CallStore {
    override suspend fun save(call: Call) {

        sqlClient.execute(
            insert(
                "calls", mapOf(
                    "id" to call.id().uuid,
                    "call_type" to call.type().name,
                    "description" to call.description(),
                    "title" to call.title(),
                    "additional_info" to call.additionalInformation(),
                    "start_date" to call.startDate(),
                    "end_date" to call.endDate(),
                    "created_at" to call.createdAt(),
                    "deleted_At" to call.deletedAt(),
                    "scoring_end_date" to call.scoringEndDate(),
                    "methodology" to call.scoringWeight()?.methodology,
                    "research_excellence" to call.scoringWeight()?.researchExcellence,
                    "support_statement" to call.scoringWeight()?.supportStatement,
                    "justification" to call.scoringWeight()?.justification,
                    "gains_outputs" to call.scoringWeight()?.gainsOutputs,
                    "merit" to call.scoringWeight()?.merit,
                    "societal_challenge" to call.scoringWeight()?.societalChallenge,
                )
            ) {
                onConflict(
                    columns = listOf("id"),
                    action = OnConflict.DoUpdate(
                        mapOf(
                            "description" to call.description(),
                            "title" to call.title(),
                            "additional_info" to call.additionalInformation(),
                            "start_date" to call.startDate(),
                            "end_date" to call.endDate(),
                            "deleted_At" to call.deletedAt(),
                            "scoring_end_date" to call.scoringEndDate(),
                            "methodology" to call.scoringWeight()?.methodology,
                            "research_excellence" to call.scoringWeight()?.researchExcellence,
                            "support_statement" to call.scoringWeight()?.supportStatement,
                            "justification" to call.scoringWeight()?.justification,
                            "gains_outputs" to call.scoringWeight()?.gainsOutputs,
                            "merit" to call.scoringWeight()?.merit,
                            "societal_challenge" to call.scoringWeight()?.societalChallenge
                        )
                    )
                )
            }
        )
    }
}
