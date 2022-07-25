package org.synthesis.account.manage

import java.time.LocalDateTime
import org.synthesis.account.*
import org.synthesis.account.manage.store.AccountStore
import org.synthesis.account.registration.UserAccountRegistrationCredentials
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.keycloak.KeycloakExceptions
import org.synthesis.keycloak.KeycloakRealm
import org.synthesis.keycloak.admin.KeycloakAccountStatus
import org.synthesis.keycloak.admin.KeycloakAdminApiClient
import org.synthesis.keycloak.admin.KeycloakUserCredentials

data class CreateUserAccountRequest(
    val email: String,
    val groups: List<String>,
    val fullName: UserFullName,
    val attributes: UserAccountAttributes,
    val credentials: UserAccountRegistrationCredentials
)

data class UpdateUserAccountRequest(
    val id: UserAccountId,
    val fullName: UserFullName,
    val attributes: UserAccountAttributes
)

interface UserAccountProvider {
    /**
     * @throws [UserAccountException.AlreadyRegistered]
     * @throws [UserAccountException.RegistrationFailed]
     * @throws [IllegalStateException]
     */
    suspend fun create(data: CreateUserAccountRequest): UserAccountId

    /**
     * @throws [UserAccountException.NotFound]
     * @throws [UserAccountException.UpdateFailed]
     * @throws [IllegalStateException]
     */
    suspend fun update(data: UpdateUserAccountRequest)

    /**
     * @throws [UserAccountException.NotFound]
     * @throws [UserAccountException.UpdateFailed]
     * @throws [IllegalStateException]
     */
    suspend fun ban(id: UserAccountId, withReason: String)

    /**
     * @throws [UserAccountException.NotFound]
     * @throws [UserAccountException.UpdateFailed]
     * @throws [IllegalStateException]
     */
    suspend fun unban(id: UserAccountId)

    /**
     * @throws [UserAccountException.NotFound]
     * @throws [UserAccountException.UpdateFailed]
     * @throws [IllegalStateException]
     */
    suspend fun promote(id: UserAccountId, group: String)

    /**
     * @throws [UserAccountException.NotFound]
     * @throws [UserAccountException.UpdateFailed]
     * @throws [IllegalStateException]
     */
    suspend fun demote(id: UserAccountId, group: String)
}

class KeycloakUserAccountProvider(
    private val userAccountFinder: UserAccountFinder,
    private val keycloakRealm: KeycloakRealm,
    private val keycloakClient: KeycloakAdminApiClient,
    private val accountStore: AccountStore
) : UserAccountProvider {
    override suspend fun create(data: CreateUserAccountRequest): UserAccountId = try {
        keycloakClient.import(data).also { id ->
            accountStore.add(
                UserAccount(
                    id = id,
                    realmId = keycloakRealm.value,
                    email = data.email,
                    groups = data.groups,
                    roles = listOf(),
                    fullName = data.fullName,
                    attributes = data.attributes,
                    status = UserAccountStatus.Active(),
                    synchronizedAt = null
                )
            )
        }
    } catch (e: StorageException) {
        throw UserAccountException.RegistrationFailed(data.email, e)
    }

    override suspend fun update(data: UpdateUserAccountRequest) {
        val account = userAccountFinder.find(data.id) ?: throw UserAccountException.NotFound()
        val updatedAccount = account.copy(
            fullName = data.fullName,
            attributes = data.attributes
        )

        accountStore.store(updatedAccount).also {
            keycloakClient.withCatch {
                updateUser(
                    realm = keycloakRealm,
                    userId = updatedAccount.id,
                    userFullName = updatedAccount.fullName,
                    userAttributes = updatedAccount.attributes
                )
            }
        }
    }

    override suspend fun ban(id: UserAccountId, withReason: String) {
        val account = userAccountFinder.find(id) ?: throw UserAccountException.NotFound()
        val updatedAccount = account.copy(
            status = UserAccountStatus.Banned(
                dateTime = LocalDateTime.now(),
                reason = withReason
            )
        )

        accountStore.store(updatedAccount).also {
            keycloakClient.withCatch {
                updateUserStatus(
                    realm = keycloakRealm,
                    userId = id,
                    newStatus = KeycloakAccountStatus.BANNED
                )
            }
        }
    }

    override suspend fun unban(id: UserAccountId) {
        val account = userAccountFinder.find(id) ?: throw UserAccountException.NotFound()
        val updatedAccount = account.copy(
            status = UserAccountStatus.Active()
        )

        accountStore.store(updatedAccount).also {
            keycloakClient.withCatch {
                updateUserStatus(
                    realm = keycloakRealm,
                    userId = id,
                    newStatus = KeycloakAccountStatus.ACTIVE
                )
            }
        }
    }

    override suspend fun promote(id: UserAccountId, group: String) {
        val account = userAccountFinder.find(id) ?: throw UserAccountException.NotFound()
        val currentGroups = account.groups.toMutableList()

        currentGroups.add(group)

        val updatedAccount = account.copy(
            groups = currentGroups.distinct()
        )

        accountStore.store(updatedAccount).also {
            keycloakClient.promote(keycloakRealm, id, group)
        }
    }

    override suspend fun demote(id: UserAccountId, group: String) {
        val account = userAccountFinder.find(id) ?: throw UserAccountException.NotFound()
        val currentGroups = account.groups.toMutableList()

        currentGroups.remove(group)

        val updatedAccount = account.copy(
            groups = currentGroups.distinct()
        )

        accountStore.store(updatedAccount).also {
            keycloakClient.demote(keycloakRealm, id, group)

            /**
             * If the user is not a member of the coordinator/moderator groups, then it is necessary to remove the
             * institutionId attribute.
             */
            if (!updatedAccount.hasInstitutionRelatedGroup() && account.attributes.institutionId != null) {
                update(
                    UpdateUserAccountRequest(
                        id = account.id,
                        fullName = account.fullName,
                        attributes = updatedAccount.attributes.copy(
                            institutionId = null
                        )
                    )
                )
            }

            /**
             * We do the same with groups that relate to the context of the country.
             */
            if (!updatedAccount.hasCountryRelatedRoles() && account.attributes.countryCode != null) {
                update(
                    UpdateUserAccountRequest(
                        id = account.id,
                        fullName = account.fullName,
                        attributes = updatedAccount.attributes.copy(
                            countryCode = null
                        )
                    )
                )
            }
        }
    }

    /**
     * @throws [KeycloakExceptions.UserAlreadyExists]
     * @throws [KeycloakExceptions.OperationFailed]
     */
    private suspend fun KeycloakAdminApiClient.import(data: CreateUserAccountRequest): UserAccountId =
        withCatch {
            importUser(
                realm = keycloakRealm,
                userEmail = data.email,
                userFullName = data.fullName,
                userGroups = data.groups,
                userAttributes = data.attributes,
                userCredentials = when (data.credentials) {
                    is UserAccountRegistrationCredentials.ClearPassword -> KeycloakUserCredentials.ClearPassword(
                        temporary = false,
                        password = data.credentials.password
                    )
                    is UserAccountRegistrationCredentials.HashedPassword -> KeycloakUserCredentials.HashedPassword(
                        temporary = false,
                        hash = data.credentials.hash,
                        algorithm = data.credentials.algorithm
                    )
                }
            )
        }

    private suspend fun <R> KeycloakAdminApiClient.withCatch(code: suspend KeycloakAdminApiClient.() -> R): R = try {
        code()
    } catch (e: KeycloakExceptions.UserNotExists) {
        throw UserAccountException.NotFound()
    } catch (e: KeycloakExceptions.UserAlreadyExists) {
        throw UserAccountException.AlreadyRegistered()
    } catch (e: Exception) {
        throw UserAccountException.OperationFailed(e)
    }
}
