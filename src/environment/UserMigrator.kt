package org.synthesis.environment

import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import java.time.LocalDateTime
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.synthesis.account.*
import org.synthesis.account.registration.RegistrationHandler
import org.synthesis.account.registration.UserAccountRegistrationCredentials
import org.synthesis.account.registration.UserAccountRegistrationData
import org.synthesis.infrastructure.persistence.querybuilder.*
import org.synthesis.infrastructure.serializer.JacksonSerializer
import org.synthesis.institution.InstitutionId
import org.synthesis.keycloak.KeycloakRealm

private data class UserToMigration(
    var account: UserAccount,
    val migratedAt: LocalDateTime?,
    val passwordHash: String
)

private data class OldUserData(
    val fullName: UserFullName
)

class UserMigrator(
    private val registrationHandler: RegistrationHandler,
    private val sqlClient: PgPool,
    private val realm: KeycloakRealm,
    private val logger: Logger
) {
    fun execute() = runBlocking {
        loadUsers().forEach { user ->

            /** In the old version, the attributes were stored in different tables, so let's try to find them */
            user.account = user.account.copy(
                attributes = UserAccountAttributes(
                    orcId = loadOrcId(user.account.id),
                    institutionId = loadInstitutionId(user.account.id),
                    gender = Gender.OTHER,
                    relatedInstitutionId = null,
                    birthDate = null,
                    nationality = null,
                    countryOtherInstitution = null
                )
            )

            val registeredId = register(user)

            /** System users, skip */
            if (user.account.groups.size == 1) {
                if (registeredId != null) {
                    markAsMigrated(user.account.id, registeredId)

                    logger.info("User `${user.account.id.uuid}` successful migrated with id `${registeredId.uuid}`")
                }
            } else {
                markAsMigrated(user.account.id, null)

                logger.info(
                    "User `${user.account.id.uuid}` have to many groups (`${user.account.groups.size}`), skipped`"
                )
            }
        }
    }

    private suspend fun markAsMigrated(id: UserAccountId, newUserId: UserAccountId?) = sqlClient.execute(
        update(
            on = "__auth_users_dump",
            rows = mapOf(
                "migrated_at" to LocalDateTime.now(),
                "migrated_id" to newUserId?.uuid
            )
        ) {
            where {
                "id" eq id.uuid
            }
        }
    )

    private suspend fun register(command: UserToMigration): UserAccountId? = try {
        registrationHandler.handle(
            UserAccountRegistrationData(
                email = command.account.email,
                groups = command.account.groups,
                fullName = command.account.fullName,
                attributes = command.account.attributes,
                credentials = UserAccountRegistrationCredentials.HashedPassword(
                    hash = command.passwordHash
                )
            ),
            false
        )
    } catch (e: Exception) {

        logger.info("User `${command.account.id.uuid}` migration error: ${e.message}")

        null
    }

    private suspend fun loadOrcId(id: UserAccountId): OrcId? {
        val row = sqlClient.fetchOne(
            select("__requesters_dump") {
                where {
                    "id" eq id.uuid
                }
            }
        ) ?: return null

        val value = row.getString("orc_id")

        return value?.let { OrcId(it) }
    }

    private suspend fun loadInstitutionId(id: UserAccountId): InstitutionId? {
        val row = sqlClient.fetchOne(
            select("__institutions_coordinators_dump") {
                where {
                    "id" eq id.uuid
                }
            }
        ) ?: return null

        val value = row.getString("institution_id")

        return value?.let { InstitutionId.fromString(it) }
    }

    private suspend fun loadUsers(): List<UserToMigration> = sqlClient.fetchAll(
        select(
            from = "__auth_users_dump as a_dump",
            columns = listOf("a_dump.*", "r_dump.orc_id")
        ) {
            leftJoinOld("__requesters_dump as r_dump", "a_dump.id = r_dump.id")

            where {
                "a_dump.migrated_at" eq null
            }
        }
    )
        .map {
            it.map()
        }
        .toList()

    private fun Row.map(): UserToMigration {
        val fullNameData = JacksonSerializer.unserialize(
            getString("user_data"),
            OldUserData::class.java
        )

        return UserToMigration(
            account = UserAccount(
                id = UserAccountId(getUUID("id")),
                realmId = realm.value,
                email = getString("email"),
                groups = getArrayOfStrings("roles").mapNotNull {
                    when (it) {
                        "REQUESTER" -> "requester"
                        "INSTITUTE_MODERATOR" -> "institution moderator"
                        "ADMINISTRATOR" -> "administrator"
                        "VA_COORDINATOR" -> "va coordinator"
                        else -> null
                    }
                },
                roles = listOf(),
                fullName = fullNameData.fullName,
                attributes = UserAccountAttributes(
                    orcId = getString("orc_id")?.let { OrcId(it.replace("https://orcid.org/", "")) },
                    institutionId = null,
                    relatedInstitutionId = null,
                    gender = Gender.OTHER,
                    birthDate = null,
                    nationality = null,
                    countryOtherInstitution = null
                ),
                synchronizedAt = null,
                status = if (getLocalDateTime("disable_date") == null) {
                    UserAccountStatus.Active()
                } else {
                    UserAccountStatus.Banned(
                        dateTime = getLocalDateTime("disable_date"),
                        reason = getString("disable_Reason")
                    )
                }
            ),
            migratedAt = getLocalDateTime("migrated_at"),
            passwordHash = getString("password")
        )
    }
}
