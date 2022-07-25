package org.synthesis.calls.request.ta.scoring

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import org.synthesis.account.UserAccountId
import org.synthesis.calls.request.CallRequestId
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.infrastructure.serializer.Serializer

class ScoreFinder(
    private val sqlClient: SqlClient,
    private val serializer: Serializer = JacksonSerializer
) {
    suspend fun find(scoreFormId: ScoreFormId): ScoreResponse? = sqlClient.fetchOne(
        select("scoring_form") {
            where {
                "id" eq scoreFormId.id
            }
        }
    )?.hydrate()

    private fun Row.hydrate() = ScoreResponse(
        id = ScoreFormId(getUUID("id")),
        requestId = CallRequestId(getUUID("request_id")),
        form = serializer.unserialize(getString("form"), DynamicForm::class.java),
        scorerId = UserAccountId(getUUID("scorer_id")),
        deletedAt = getLocalDateTime("deleted_at")
    )
}
