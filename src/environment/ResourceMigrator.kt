package org.synthesis.environment

import io.vertx.pgclient.PgPool
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.synthesis.infrastructure.persistence.querybuilder.execute
import org.synthesis.infrastructure.persistence.querybuilder.select
import org.synthesis.infrastructure.persistence.querybuilder.update
import org.synthesis.keycloak.KeycloakExceptions
import org.synthesis.keycloak.api.KeycloakClient
import org.synthesis.keycloak.api.KeycloakResourceId
import org.synthesis.keycloak.api.Resource
import org.synthesis.keycloak.api.Scope

class ResourceMigrator(
    private val sqlClient: PgPool,
    private val keycloakClient: KeycloakClient,
    private val logger: Logger
) {

    fun execute() = runBlocking {
        migrateInstitutions()
        migrateCallRequests()
    }

    private suspend fun migrateCallRequests() {

        val scopes = listOf(Scope("VaCall Request"))

        sqlClient.execute(
            select("requests") {
                where {
                    "resource_id" eq null
                }
            }
        ).forEach { row ->
            val id = row.getUUID("id")
            val resourceId = try {
                keycloakClient.create(
                    Resource(
                        id = id.toString(),
                        name = row.getString("title"),
                        type = "VaCall Request",
                        scopes = scopes,
                        attributes = mapOf(
                            "id" to id.toString(),
                            "author_id" to row.getUUID("requester_id").toString()
                        )
                    )
                )
            } catch (e: KeycloakExceptions.EntityAlreadyExists) {
                // @todo: fix me
                KeycloakResourceId(id)
            }

            sqlClient.execute(
                update("requests", mapOf("resource_id" to resourceId.id)) {
                    where {
                        "id" eq id
                    }
                }
            )

            logger.info("Resource for request `$id` successful created in Keycloak")
        }
    }

    private suspend fun migrateInstitutions() {
        sqlClient.execute(
            select("institutions") {
                where {
                    "resource_id" eq null
                }
            }
        ).forEach { row ->
            val institutionId = row.getString("id")
            val institutionTitle = row.getString("title")

            keycloakClient.create(
                Resource(
                    id = institutionId,
                    name = institutionTitle,
                    type = "Institutions",
                    scopes = listOf(Scope("Institutions")),
                    attributes = mapOf(
                        "id" to institutionId
                    )
                )
            ).let { resource ->
                sqlClient.execute(
                    update("institutions", mapOf("resource_id" to resource.id)) {
                        where {
                            "id" eq institutionId
                        }
                    }
                )

                logger.info("Resource for institution `$institutionId` successful created in Keycloak")
            }
        }
    }
}
