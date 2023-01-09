package org.synthesis.institution.coordinator

import kotlinx.coroutines.flow.Flow
import org.synthesis.account.UserAccount
import org.synthesis.account.UserAccountFinder
import org.synthesis.account.UserAccountId
import org.synthesis.account.manage.UpdateUserAccountRequest
import org.synthesis.account.manage.UserAccountProvider
import org.synthesis.calls.request.ta.store.TaCallRequestStore
import org.synthesis.calls.request.va.store.VaCallRequestStore
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.institution.InstitutionId

class CoordinatorManager(
    private val userAccountFinder: UserAccountFinder,
    private val userAccountProvider: UserAccountProvider,
    private val taCallRequestStore: TaCallRequestStore,
    private val vaCallRequestStore: VaCallRequestStore,
    private val coordinatorStore: PgCoordinatorStore
) {

    fun list(role: String, institutionId: InstitutionId): Flow<UserAccount> =
        userAccountFinder.findWithCriteria {
            "group_list" inArray listOf(role)
            "institution_id" eq institutionId.grid.value
        }

    suspend fun promote(id: UserAccountId, role: String, institutionId: InstitutionId) {
        val user = userAccountFinder.find(id)
            ?: throw IncorrectRequestParameters.create("userId", "User was not found")

        val request = UpdateUserAccountRequest(
            id = user.id,
            fullName = user.fullName,
            attributes = user.attributes.copy(
                institutionId = institutionId
            )
        )

        when (role) {
            "va coordinator" -> {
                vaCallRequestStore.updateCoordinator(user.id, institutionId)
                coordinatorStore.update(institutionId, user.id, "va")
            }
            "ta coordinator" -> {
                taCallRequestStore.updateCoordinator(user.id, institutionId)
                coordinatorStore.update(institutionId, user.id, "ta")
            }
        }
        userAccountProvider.update(request)
        userAccountProvider.promote(user.id, role)
    }

    suspend fun demote(id: UserAccountId, role: String) = userAccountProvider.demote(id, role)
}
