package org.synthesis.search

import org.koin.core.Koin
import org.koin.core.qualifier.named

object IncorrectIndex : Exception()

/**
 * @todo: security checks
 */
class SearchProviderLocator(
    private val koin: Koin
) {
    private val indexRelations = mapOf(
        "calls" to "CallSearchAdapter",
        "requests" to "CallRequestsSearchAdapter",
        "requestsForms" to "InstitutionCallRequestsSearchAdapter",
        "institutions" to "InstitutionSearchAdapter",
        "users" to "UsersSearchAdapter",
        "vaCoordinators" to "VaCoordinatorSearchAdapter",
        "settings" to "SettingsSearchAdapter",
        "facilities" to "FacilitySearchAdapter"
    )

    fun obtain(index: String): PostgreSqlSearchAdapter<*> = koin
        .get(
            named(indexRelations[index] ?: throw IncorrectIndex)
        )
}
