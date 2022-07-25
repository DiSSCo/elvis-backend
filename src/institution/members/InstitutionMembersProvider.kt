package org.synthesis.institution.members

import kotlinx.coroutines.flow.Flow
import org.synthesis.account.UserAccount
import org.synthesis.account.UserAccountFinder
import org.synthesis.account.UserAccountId
import org.synthesis.account.manage.UpdateUserAccountRequest
import org.synthesis.account.manage.UserAccountProvider
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.institution.InstitutionId

class InstitutionMembersProvider(
    private val userAccountFinder: UserAccountFinder,
    private val userAccountProvider: UserAccountProvider
) {
    suspend fun list(forInstitutionId: InstitutionId): Flow<UserAccount> =
        userAccountFinder.findWithCriteria {
            "related_institution_id" eq forInstitutionId.grid.value
        }

    /**
     * @throws [IncorrectRequestParameters]
     */
    suspend fun attach(id: UserAccountId, toInstitutionId: InstitutionId) {
        val user = findUser(id)

        userAccountProvider.update(
            UpdateUserAccountRequest(
                id = user.id,
                fullName = user.fullName,
                attributes = user.attributes.copy(
                    relatedInstitutionId = toInstitutionId
                )
            )
        )
    }

    /**
     * @throws [IncorrectRequestParameters]
     */
    suspend fun detach(id: UserAccountId) {
        val user = findUser(id)

        userAccountProvider.update(
            UpdateUserAccountRequest(
                id = user.id,
                fullName = user.fullName,
                attributes = user.attributes.copy(
                    relatedInstitutionId = null
                )
            )
        )
    }

    /**
     * @throws [IncorrectRequestParameters]
     */
    private suspend fun findUser(id: UserAccountId): UserAccount =
        userAccountFinder.find(id) ?: throw IncorrectRequestParameters.create(
            "userId",
            "User was not found"
        )
}
