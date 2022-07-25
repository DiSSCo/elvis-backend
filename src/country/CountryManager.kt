package org.synthesis.country

import kotlinx.coroutines.flow.Flow
import org.synthesis.account.UserAccount
import org.synthesis.account.UserAccountFinder
import org.synthesis.account.UserAccountId
import org.synthesis.account.manage.UpdateUserAccountRequest
import org.synthesis.account.manage.UserAccountProvider
import org.synthesis.infrastructure.IncorrectRequestParameters

class CountryManager(
    private val userAccountFinder: UserAccountFinder,
    private val userAccountProvider: UserAccountProvider
) {

    suspend fun taScorers(inCountry: CountryCode): Flow<UserAccount> = userAccountFinder
        .findWithCriteria {
            "group_list" inArray listOf("ta scorer")
            "country_code" eq inCountry.id
        }

    suspend fun tafAdmins(inCountry: CountryCode): Flow<UserAccount> = userAccountFinder
        .findWithCriteria {
            "group_list" inArray listOf("taf admin")
            "country_code" eq inCountry.id
        }

    /**
     * @throws [IncorrectRequestParameters]
     */
    suspend fun promote(id: UserAccountId, role: String, countryCode: CountryCode) {
        val user = findUser(id)

        val request = UpdateUserAccountRequest(
            id = user.id,
            fullName = user.fullName,
            attributes = user.attributes.copy(
                countryCode = countryCode
            )
        )

        userAccountProvider.update(request)
        userAccountProvider.promote(user.id, role)
    }

    /**
     * @throws [IncorrectRequestParameters]
     */
    suspend fun demote(id: UserAccountId, role: String) = findUser(id).let {
        userAccountProvider.demote(id, role)
    }

    /**
     * @throws [IncorrectRequestParameters]
     */
    private suspend fun findUser(id: UserAccountId) = userAccountFinder.find(id)
        ?: throw IncorrectRequestParameters.create("userId", "User was not found")
}
