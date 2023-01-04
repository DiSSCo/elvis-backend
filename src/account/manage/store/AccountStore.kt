package org.synthesis.account.manage.store

import io.vertx.sqlclient.SqlClient
import java.time.LocalDateTime
import java.util.*
import org.synthesis.account.UserAccount
import org.synthesis.account.UserAccountId
import org.synthesis.account.UserAccountStatus
import org.synthesis.country.CountryCode
import org.synthesis.infrastructure.persistence.querybuilder.*
import org.synthesis.institution.InstitutionId
import org.synthesis.institution.coordinator.CoordinatorType

interface AccountStore {
    suspend fun add(account: UserAccount)
    suspend fun store(account: UserAccount)
    suspend fun syncRoles(id: UserAccountId, roles: List<String>)
}

class DefaultAccountStore(
    private val sqlClient: SqlClient
) : AccountStore {
    override suspend fun add(account: UserAccount) {
        sqlClient.execute(
            insert(
                to = "accounts",
                rows = mapOf(
                    "id" to account.id.uuid,
                    "email" to account.email,
                    "first_name" to account.fullName.firstName,
                    "last_name" to account.fullName.lastName,
                    "group_list" to account.groups.toTypedArray(),
                    "roles_list" to listOf<String>().toTypedArray(),
                    "orc_id" to account.attributes.orcId?.id,
                    "institution_id" to account.attributes.institutionId?.grid?.value,
                    "related_institution_id" to account.attributes.relatedInstitutionId?.grid?.value,
                    "gender" to account.attributes.gender.name.lowercase(),
                    "birth_date" to account.attributes.birthDate,
                    "country_code" to account.attributes.countryCode?.id?.uppercase(),
                    "home_institution_id" to account.attributes.homeInstitutionId,
                    "nationality" to account.attributes.nationality,
                    "country_other_institution" to account.attributes.countryOtherInstitution,
                    "realm_id" to account.realmId,
                    "synchronized_at" to null
                )
            )
        )

        sqlClient.updateRelations(account)
    }

    override suspend fun store(account: UserAccount) {
        sqlClient.execute(
            update(
                on = "accounts",
                rows = mapOf(
                    "first_name" to account.fullName.firstName,
                    "last_name" to account.fullName.lastName,
                    "group_list" to account.groups.toTypedArray(),
                    "orc_id" to account.attributes.orcId?.id,
                    "institution_id" to account.attributes.institutionId?.grid?.value,
                    "related_institution_id" to account.attributes.relatedInstitutionId?.grid?.value,
                    "gender" to account.attributes.gender.name.lowercase(),
                    "birth_date" to account.attributes.birthDate,
                    "home_institution_id" to account.attributes.homeInstitutionId,
                    "nationality" to account.attributes.nationality,
                    "country_other_institution" to account.attributes.countryOtherInstitution,
                    "country_code" to account.attributes.countryCode?.id?.uppercase(),
                    "banned_at" to if (account.status is UserAccountStatus.Banned) account.status.dateTime else null,
                    "banned_with_reason" to if (account.status is UserAccountStatus.Banned) account.status.reason else null
                )
            ) {
                where {
                    "id" eq account.id.uuid
                }
            }
        )

        sqlClient.updateRelations(account)
    }

    override suspend fun syncRoles(id: UserAccountId, roles: List<String>) {
        sqlClient.execute(
            update(
                on = "accounts",
                rows = mapOf(
                    "roles_list" to roles.toTypedArray(),
                    "synchronized_at" to LocalDateTime.now()
                )
            ) {
                where {
                    "id" eq id.uuid
                }
            }
        )
    }

    /**
     * @todo: fix me
     */
    private suspend fun SqlClient.updateRelations(account: UserAccount) {
        val institutionId = account.attributes.institutionId
        val countryCode = account.attributes.countryCode

        if (institutionId != null) {
            /** VA Coordinator **/
            if ("va coordinator" in account.groups) {
                linkCoordinatorWithInstitution(account.id, institutionId, CoordinatorType.VA)
            }

            if ("va coordinator" !in account.groups) {
                unlinkCoordinatorFromInstitution(account.id, institutionId, CoordinatorType.VA)
            }

            /** TA Coordinator **/
            if ("ta coordinator" in account.groups) {
                linkCoordinatorWithInstitution(account.id, institutionId, CoordinatorType.TA)
            }

            if ("ta coordinator" !in account.groups) {
                unlinkCoordinatorFromInstitution(account.id, institutionId, CoordinatorType.TA)
            }
        }

        if (countryCode != null) {
            /** TAF Admin **/
            if ("taf admin" in account.groups) {
                linkTafAdmin(account.id, countryCode)
            }

            if ("taf admin" !in account.groups) {
                unlinkTafAdmin(account.id, countryCode)
            }

            /** TA Scorer **/
            if ("ta scorer" in account.groups) {
                linkScorer(account.id, countryCode)
            }

            if ("ta scorer" !in account.groups) {
                unlinkScorer(account.id, countryCode)
            }
        }
    }

    private suspend fun SqlClient.linkScorer(userId: UserAccountId, countryCode: CountryCode) {
        execute(
            insert(
                to = "scorers",
                rows = mapOf(
                    "id" to UUID.randomUUID(),
                    "user_id" to userId.uuid,
                    "country_code" to countryCode.id.uppercase(),
                )
            ) {
                onConflict(
                    columns = listOf("user_id", "country_code"),
                    action = OnConflict.DoNothing()
                )
            }
        )
    }

    private suspend fun SqlClient.unlinkScorer(userId: UserAccountId, countryCode: CountryCode) {
        execute(
            delete("scorers") {
                where {
                    "user_id" eq userId.uuid
                    "country_code" eq countryCode.id.uppercase()
                }
            }
        )
    }

    private suspend fun SqlClient.linkTafAdmin(userId: UserAccountId, countryCode: CountryCode) {
        execute(
            insert(
                to = "taf_administrators",
                rows = mapOf(
                    "id" to UUID.randomUUID(),
                    "user_id" to userId.uuid,
                    "country_code" to countryCode.id.uppercase(),
                )
            ) {
                onConflict(
                    columns = listOf("user_id", "country_code"),
                    action = OnConflict.DoNothing()
                )
            }
        )
    }

    private suspend fun SqlClient.unlinkTafAdmin(userId: UserAccountId, countryCode: CountryCode) {
        execute(
            delete("taf_administrators") {
                where {
                    "user_id" eq userId.uuid
                    "country_code" eq countryCode.id.uppercase()
                }
            }
        )
    }

    private suspend fun SqlClient.linkCoordinatorWithInstitution(
        userId: UserAccountId,
        institutionId: InstitutionId,
        access: CoordinatorType
    ) {
        execute(
            insert(
                to = "institutions_coordinators",
                rows = mapOf(
                    "id" to UUID.randomUUID(),
                    "user_id" to userId.uuid,
                    "institution_id" to institutionId.grid.value,
                    "access" to access.name.lowercase()
                )
            ) {
                onConflict(
                    columns = listOf("user_id", "institution_id", "access"),
                    action = OnConflict.DoNothing()
                )
            }
        )
    }

    private suspend fun SqlClient.unlinkCoordinatorFromInstitution(
        userId: UserAccountId,
        institutionId: InstitutionId,
        access: CoordinatorType
    ) {
        execute(
            delete("institutions_coordinators") {
                where {
                    "user_id" eq userId.uuid
                    "institution_id" eq institutionId.grid.value
                    "access" eq access.name.lowercase()
                }
            }
        )
    }
}
