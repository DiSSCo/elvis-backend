package org.synthesis.account

import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.synthesis.account.manage.UserManageException
import org.synthesis.country.CountryCode
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.infrastructure.persistence.querybuilder.CriteriaBuilder
import org.synthesis.infrastructure.persistence.querybuilder.fetchAll
import org.synthesis.infrastructure.persistence.querybuilder.fetchOne
import org.synthesis.infrastructure.persistence.querybuilder.select
import org.synthesis.institution.InstitutionId
import org.synthesis.keycloak.KeycloakExceptions
import org.synthesis.keycloak.KeycloakRealm
import org.synthesis.keycloak.admin.KeycloakAdminApiClient

interface UserAccountFinder {
    /**
     * @throws [StorageException.InteractingFailed]
     */
    suspend fun find(id: UserAccountId): UserAccount?

    /**
     * @throws [StorageException.InteractingFailed]
     * @throws [UserManageException.OperationFailed]
     * @throws [KeycloakExceptions.UserNotExists]
     * @throws [IllegalStateException]
     */
    fun find(query: String, offset: Int, limit: Int): Flow<UserAccount>

    /**
     * @throws [StorageException.InteractingFailed]
     */
    fun findWithCriteria(code: CriteriaBuilder.() -> Unit): Flow<UserAccount>
}

class KeycloakUserAccountFinder(
    private val keycloakRealm: KeycloakRealm,
    private val keycloakClient: KeycloakAdminApiClient,
    private val sqlClient: SqlClient
) : UserAccountFinder {

    override fun find(query: String, offset: Int, limit: Int): Flow<UserAccount> = flow {
        keycloakClient.findUsers(
            realm = keycloakRealm,
            query = query,
            offset = offset,
            limit = limit
        ).second.forEach { userAccount ->
            find(userAccount.id)?.let { emit(it) }
        }
    }

    override suspend fun find(id: UserAccountId): UserAccount? = sqlClient.fetchOne(
        select("accounts") {
            where { "id" eq id.uuid }
        }
    )?.hydrate()

    override fun findWithCriteria(code: CriteriaBuilder.() -> Unit): Flow<UserAccount> = sqlClient.fetchAll(
        select("accounts") {
            where(code)
        }
    ).map {
        it.hydrate()
    }

    private fun Row.hydrate(): UserAccount {
        val bannedAt = getLocalDateTime("banned_at")

        return UserAccount(
            id = UserAccountId(getUUID("id")),
            realmId = getString("realm_id"),
            email = getString("email"),
            groups = getArrayOfStrings("group_list").toList(),
            roles = getArrayOfStrings("roles_list").toList(),
            fullName = UserFullName(
                firstName = getString("first_name"),
                lastName = getString("last_name")
            ),
            attributes = UserAccountAttributes(
                orcId = getString("orc_id")?.let { OrcId(it) },
                institutionId = getString("institution_id")?.let { InstitutionId.fromString(it) },
                relatedInstitutionId = getString("related_institution_id")?.let { InstitutionId.fromString(it) },
                gender = Gender.valueOf(getString("gender").uppercase()),
                birthDate = getLocalDate("birth_date"),
                countryCode = getString("country_code")?.let { CountryCode(it.uppercase()) },
                nationality = getString("nationality"),
                homeInstitutionId = getString("home_institution_id"),
                countryOtherInstitution = getString("country_other_institution")
            ),
            synchronizedAt = getLocalDateTime("synchronized_at"),
            status = if (bannedAt == null) {
                UserAccountStatus.Active()
            } else {
                UserAccountStatus.Banned(
                    dateTime = bannedAt,
                    reason = getString("banned_with_reason")
                )
            }
        )
    }
}
