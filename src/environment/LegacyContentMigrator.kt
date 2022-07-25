package org.synthesis.environment

import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import java.time.LocalDateTime
import java.util.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.synthesis.infrastructure.persistence.querybuilder.*
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.infrastructure.serializer.Serializer

/**
 * @todo: remove after deployment
 */
class LegacyContentMigrator(
    private val sqlClient: PgPool,
    private val logger: Logger,
    private val serializer: Serializer = JacksonSerializer
) {
    fun execute(): Unit = runBlocking {
        sqlClient.transaction {
            /** Update call requests **/
            val oldUsers = fetchAll(
                select("__auth_users_dump") {
                    where {
                        "replaced_at" eq null
                    }
                }
            )

            oldUsers.toList().forEach { oldUserRow ->
                val oldUserEmail = oldUserRow.getString("email")
                val oldUserId = oldUserRow.getUUID("id")

                val newUserRow = fetchOne(
                    select("accounts") {
                        where {
                            "email" eq oldUserEmail
                        }
                    }
                )

                if (newUserRow != null) {
                    val newUserId = newUserRow.getUUID("id")

                    execute(
                        update("requests", mapOf("requester_id" to newUserId)) {
                            where {
                                "requester_id" eq oldUserId
                            }
                        }
                    )

                    execute(
                        update("requests_institution_forms", mapOf("coordinator_id" to newUserId)) {
                            where {
                                "coordinator_id" eq oldUserId
                            }
                        }
                    )

                    logger.info("User `$oldUserEmail` successful migrated")
                } else {
                    logger.info("No matches found for user `$oldUserEmail` in the new version of the database")
                }

                execute(
                    update("__auth_users_dump", mapOf("replaced_at" to LocalDateTime.now())) {
                        where {
                            "id" eq oldUserId
                        }
                    }
                )
            }

            /** update call request comments **/
            val commentsRow = fetchAll(
                select("__comments") {
                    where {
                        "migrated_at" eq null
                    }
                }
            )

            for (oldCommentRow in commentsRow.toList()) {
                val commentId = oldCommentRow.getUUID("id")
                val authorId = serializer
                    .unserialize(oldCommentRow.getString("author_data"), Author::class.java)
                    .newId()

                if (authorId != null) {
                    execute(
                        insert(
                            "comments", mapOf(
                                "id" to commentId,
                                "thread_id" to oldCommentRow.getUUID("thread_id"),
                                "author_id" to authorId,
                                "reply_to" to oldCommentRow.getUUID("reply_to"),
                                "format" to oldCommentRow.getString("format"),
                                "message" to oldCommentRow.getString("message"),
                                "created_at" to oldCommentRow.getLocalDateTime("created_at"),
                                "deleted_at" to oldCommentRow.getLocalDateTime("deleted_at")
                            )
                        )
                    )
                }

                execute(
                    update("__comments", mapOf("migrated_at" to LocalDateTime.now())) {
                        where {
                            "id" eq commentId
                        }
                    }
                )
            }

            /** update call request attachments **/
            val attachmentsRow = fetchAll(
                select("__requests_attachments") {
                    where {
                        "migrated_at" eq null
                    }
                }
            )

            for (attachmentRow in attachmentsRow.toList()) {
                val attachmentId = attachmentRow.getUUID("id")
                val authorId = serializer
                    .unserialize(attachmentRow.getString("author_data"), Author::class.java)
                    .newId()

                if (authorId != null) {
                    execute(
                        insert(
                            "requests_attachments", mapOf(
                                "id" to attachmentId,
                                "call_request_id" to attachmentRow.getUUID("request_id"),
                                "stored_file_id" to attachmentRow.getUUID("stored_file_id"),
                                "owner_id" to authorId,
                                "institution_id" to attachmentRow.getString("institution_id"),
                                "created_at" to attachmentRow.getLocalDateTime("created_at")
                            )
                        )
                    )

                    execute(
                        update("__requests_attachments", mapOf("migrated_at" to LocalDateTime.now())) {
                            where {
                                "id" eq attachmentId
                            }
                        }
                    )
                }
            }

            /** Fix requests payload */
            fixFormData("requests")
            fixFormData("requests_institution_forms")

            sqlClient
                .preparedQuery(
                    """
                        INSERT INTO institution_moderators 
                        SELECT user_id as id, institution_id FROM institutions_coordinators WHERE access = 'va'
                        ON CONFLICT DO NOTHING
                    """.trimIndent()
                )
                .execute()
                .await()
        }
    }

    private suspend fun SqlConnection.fixFormData(table: String) {
        fetchAll(select(table)).toList().forEach {
            val payload = it.getString("form")
                .replace("org.synthesis.callrequest.FieldValue\$Text", "String")
                .replace("org.synthesis.callrequest.FieldValue\$Checkbox", "Boolean")

            execute(
                update(table, mapOf("form" to payload)) {
                    where {
                        "id" eq it.getUUID("id")
                    }
                }
            )
        }
    }

    private suspend fun Author.newId(): UUID? {
        val preparedQuery = sqlClient.preparedQuery(
            "SELECT id from accounts where email = (select email from __auth_users_dump where id = $1);"
        )

        return preparedQuery
            .execute(Tuple.of(UUID.fromString(id)))
            .await()
            .firstOrNull()
            ?.getUUID("id")
    }

    private data class Author(
        val id: String,
        val fullName: String,
        val roles: List<String>
    )
}
