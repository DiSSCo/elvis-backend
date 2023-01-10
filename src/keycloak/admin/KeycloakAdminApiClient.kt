package org.synthesis.keycloak.admin

import kotlinx.coroutines.CoroutineDispatcher
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jboss.resteasy.client.jaxrs.ClientHttpEngineBuilder43
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.admin.client.resource.UsersResource
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.Logger
import org.synthesis.account.*
import org.synthesis.keycloak.KeycloakConfiguration
import org.synthesis.keycloak.KeycloakExceptions
import org.synthesis.keycloak.KeycloakRealm

interface KeycloakAdminApiClient {
    /**
     * Import a new user.
     *
     * @throws [KeycloakExceptions.UserAlreadyExists]
     * @throws [KeycloakExceptions.OperationFailed]
     */
    suspend fun importUser(
        realm: KeycloakRealm,
        userEmail: String,
        userFullName: UserFullName,
        userAttributes: UserAccountAttributes,
        userCredentials: KeycloakUserCredentials,
        userGroups: List<String>
    ): UserAccountId

    /**
     * Update user information.
     *
     * @throws [KeycloakExceptions.UserNotExists]
     * @throws [KeycloakExceptions.OperationFailed]
     */
    suspend fun updateUser(
        realm: KeycloakRealm,
        userId: UserAccountId,
        userFullName: UserFullName,
        userAttributes: UserAccountAttributes
    )

    /**
     * Add user to group
     *
     * @throws [KeycloakExceptions.UserNotExists]
     * @throws [KeycloakExceptions.OperationFailed]
     */
    suspend fun promote(realm: KeycloakRealm, userId: UserAccountId, group: String)

    /**
     * Remove user from group
     *
     * @throws [KeycloakExceptions.UserNotExists]
     * @throws [KeycloakExceptions.OperationFailed]
     */
    suspend fun demote(realm: KeycloakRealm, userId: UserAccountId, group: String)

    /**
     * Update user activity status.
     *
     * @throws [KeycloakExceptions.UserNotExists]
     * @throws [KeycloakExceptions.OperationFailed]
     */
    suspend fun updateUserStatus(realm: KeycloakRealm, userId: UserAccountId, newStatus: KeycloakAccountStatus)

    /**
     * Find user by id.
     *
     * @throws [KeycloakExceptions.UserNotExists]
     */
    suspend fun findUser(realm: KeycloakRealm, id: UserAccountId): UserAccount?

    /**
     * Find users by specified query (inc email, full name, etc.)
     *
     * @throws [KeycloakExceptions.UserNotExists]
     */
    suspend fun findUsers(realm: KeycloakRealm, query: String?, offset: Int, limit: Int): Pair<Int, List<UserAccount>>
}

class DefaultKeycloakAdminApiClient(
    private val configuration: KeycloakConfiguration,
    private val logger: Logger,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : KeycloakAdminApiClient {

    private val adminClient: Keycloak by lazy {
        KeycloakBuilder.builder()
            .serverUrl(configuration.serverUrl)
            .realm("master")
            .resteasyClient(
                ResteasyClientBuilder().apply {
                    httpEngine(ClientHttpEngineBuilder43().resteasyClientBuilder(this).build())
                    connectionPoolSize(1)
                }.build()
            )
            .build()
    }

    /**
     * Special crutch: Keyloak does not allow adding a user to a group by group name. He needs an ID.
     * This map contains the association between name and identifier and identifier and name.
     */
    private lateinit var groupRelations: Map<String, String>

    override suspend fun importUser(
        realm: KeycloakRealm,
        userEmail: String,
        userFullName: UserFullName,
        userAttributes: UserAccountAttributes,
        userCredentials: KeycloakUserCredentials,
        userGroups: List<String>
    ): UserAccountId = adminClient.withCatch(realm) {
        val usersResource = users(realm)

        usersResource.findOne(userEmail)?.let {
            throw KeycloakExceptions.UserAlreadyExists(realm, userEmail)
        }

        usersResource.add(
            UserRepresentation().apply {
                email = userEmail
                username = userEmail
                isEmailVerified = true
                firstName = userFullName.firstName
                lastName = userFullName.lastName
                attributes = userAttributes.toKeycloakRepresentation()
                credentials = listOf(userCredentials.toKeycloakRepresentation())
                groups = userGroups
                isEnabled = true
            }
        )
    }

    override suspend fun updateUser(
        realm: KeycloakRealm,
        userId: UserAccountId,
        userFullName: UserFullName,
        userAttributes: UserAccountAttributes
    ) = adminClient.withCatch(realm) {
        executeUserUpdate(realm, userId) { userAccount ->
            userAccount.copy(
                fullName = userFullName,
                attributes = userAttributes
            )
        }
    }

    override suspend fun updateUserStatus(
        realm: KeycloakRealm,
        userId: UserAccountId,
        newStatus: KeycloakAccountStatus
    ) = adminClient.withCatch(realm) {
        executeUserUpdate(realm, userId) { userAccount ->
            userAccount.copy(
                status = if (newStatus == KeycloakAccountStatus.ACTIVE) {
                    UserAccountStatus.Active()
                } else {
                    UserAccountStatus.Inactive()
                }
            )
        }
    }

    override suspend fun promote(realm: KeycloakRealm, userId: UserAccountId, group: String) =
        users(realm).findOne(userId)?.addGroup(matchGroup(realm, group))
            ?: throw KeycloakExceptions.UserNotExists(realm, userId)

    override suspend fun demote(realm: KeycloakRealm, userId: UserAccountId, group: String) =
        users(realm).findOne(userId)?.removeGroup(matchGroup(realm, group))
            ?: throw KeycloakExceptions.UserNotExists(realm, userId)

    override suspend fun findUser(realm: KeycloakRealm, id: UserAccountId): UserAccount? =
        adminClient.withCatch(realm) {
            users(realm)
                .findOne(id)
                ?.toRepresentation()
                ?.applicationUser(realm)
        }

    override suspend fun findUsers(
        realm: KeycloakRealm,
        query: String?,
        offset: Int,
        limit: Int
    ): Pair<Int, List<UserAccount>> = adminClient.withCatch(realm) {
        users(realm).findAll(query, offset, limit)
    }

    /**
     * Add user to group.
     */
    private suspend fun UserResource.addGroup(keycloakGroup: String) = withContext(dispatcher) {
        joinGroup(keycloakGroup)
    }

    /**
     * Remove user from group.
     */
    private suspend fun UserResource.removeGroup(keycloakGroup: String) = withContext(dispatcher) {
        leaveGroup(keycloakGroup)
    }

    /**
     * Getting a resource for working with users in the context of the current realm.
     * Since the user resource does not contain a link to the realm, we will create a proxy class and wrap the
     * users resource in it.
     */
    private suspend fun users(inRealm: KeycloakRealm) = withContext(dispatcher) {
        WrappedUsersResource(
            resource = realm(inRealm).users(),
            context = inRealm
        )
    }

    /**
     * Search for a user by Email.
     */
    private suspend fun WrappedUsersResource.findOne(email: String) = withContext(dispatcher) {
        resource.search(email)
            .firstOrNull()
            ?.applicationUser(context)
    }

    /**
     * Search for a user by ID.
     */
    private suspend fun WrappedUsersResource.findOne(id: UserAccountId) = withContext(dispatcher) {
        resource.get(id.uuid.toString())
    }

    /**
     * Search for users with the specified query (A String contained in username, first or last name, or email).
     */
    private suspend fun WrappedUsersResource.findAll(query: String?, offset: Int, limit: Int) =
        withContext(dispatcher) {
            val count = async {
                resource.count(query)
            }

            val collection = async {
                resource.search(query, offset, limit, false)
                    ?.map { it.applicationUser(context) }
                    ?.toList()
                    ?: listOf()
            }

            Pair(count.await(), collection.await())
        }

    /**
     * Adding a new user.
     */
    private suspend fun WrappedUsersResource.add(user: UserRepresentation) = withContext(dispatcher) {
        val response = resource.create(user)

        UserAccountId(
            UUID.fromString(
                CreatedResponseUtil.getCreatedId(response)
            )
        )
    }

    /**
     * Update existing user.
     */
    private suspend fun UserResource.save(user: UserRepresentation) = withContext(dispatcher) {
        update(user)
    }

    /**
     * Perform update user request.
     *
     * @throws [KeycloakExceptions.UserNotExists]
     * @throws [KeycloakExceptions.OperationFailed]
     */
    private suspend fun executeUserUpdate(
        realm: KeycloakRealm,
        userId: UserAccountId,
        mutator: (UserAccount) -> UserAccount
    ) {
        val usersResource = users(realm)

        val userResource = usersResource.findOne(userId) ?: throw KeycloakExceptions.UserNotExists(realm, userId)

        userResource.save(
            mutator(
                userResource.toRepresentation().applicationUser(realm)
            ).userRepresentation()
        )
    }

    /**
     * Get the group ID on the Keycloak side
     */
    private suspend fun matchGroup(id: KeycloakRealm, group: String): String {
        return groups(id)[group] ?: error("Incorrect group: $group")
    }

    /**
     * Gets a map for matching user groups.
     *
     * id -> name
     * name -> id
     */
    private suspend fun groups(id: KeycloakRealm): Map<String, String> {
        if (!this::groupRelations.isInitialized) {
            groupRelations = realm(id).groups().groups().map {
                it.id to it.name
                it.name to it.id
            }.toMap()
        }

        return groupRelations
    }

    /**
     * Getting the resource of the current realm.
     */
    private suspend fun realm(id: KeycloakRealm) = withContext(dispatcher) { adminClient.realm(id.value) }

    /**
     * @throws [KeycloakExceptions.OperationFailed]
     */
    private suspend fun <R> Keycloak.withCatch(
        realm: KeycloakRealm,
        code: suspend Keycloak.() -> R
    ): R = try {
        code()
    } catch (e: KeycloakExceptions) {
        throw e
    } catch (e: Exception) {
        logger.info("Keycloak operation failed (`${realm.value}:${configuration.serverUrl}`) : ${e.message}")

        if (e.isNotRunning()) {
            withCatch(realm, code)
        } else {
            throw KeycloakExceptions.OperationFailed(realm, e)
        }
    }

    private fun Exception.isNotRunning() = message?.contains("Unable to invoke request") ?: false

    private data class WrappedUsersResource(
        val resource: UsersResource,
        val context: KeycloakRealm
    )
}
