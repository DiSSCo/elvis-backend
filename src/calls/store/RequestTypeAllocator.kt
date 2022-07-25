package org.synthesis.calls.store

import io.vertx.sqlclient.SqlClient
import java.util.*
import org.synthesis.calls.CallType
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select

class RequestTypeAllocator(
    private val sqlClient: SqlClient
) {
    suspend fun allocate(id: UUID): CallType? = sqlClient.fetchOne(
        select("requests") {
            where {
                "id" eq id
            }
        }
    )?.let {
        val type = it.getString("type")
            .replace("Transnational Access", "TA")
            .replace("Virtual Access", "VA")
            .toUpperCase()

        CallType.valueOf(type)
    }
}
