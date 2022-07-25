package org.synthesis.environment

import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.synthesis.account.UserAccountId
import org.synthesis.account.manage.UserAccountProvider
import org.synthesis.infrastructure.persistence.querybuilder.fetchAll
import org.synthesis.infrastructure.persistence.querybuilder.select

/**
 * Adjustment of current user groups.
 */
class GroupAdjustmentMigrator(
    private val userAccountProvider: UserAccountProvider,
    private val sqlClient: SqlClient
) {
    fun execute(): Unit = runBlocking {
        val moderators = sqlClient.fetchAll(select("institution_moderators"))

        for (moderatorRow in moderators.toList()) {
            val id = UserAccountId(moderatorRow.getUUID("id"))

            userAccountProvider.promote(id, "institution moderator")
        }
    }
}
