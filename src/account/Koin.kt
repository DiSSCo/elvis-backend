package org.synthesis.account

import io.vertx.sqlclient.Row
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.slf4j.LoggerFactory
import org.synthesis.account.manage.KeycloakUserAccountProvider
import org.synthesis.account.manage.UserAccountProvider
import org.synthesis.account.manage.store.AccountStore
import org.synthesis.account.manage.store.DefaultAccountStore
import org.synthesis.account.registration.KeycloakRegistrationHandler
import org.synthesis.account.registration.RegistrationHandler
import org.synthesis.search.PostgreSqlSearchAdapter

val accountModule = module {

    single<UserAccountFinder> {
        KeycloakUserAccountFinder(
            sqlClient = get(),
            keycloakRealm = get(),
            keycloakClient = get()
        )
    }

    single<AccountStore> {
        DefaultAccountStore(
            sqlClient = get()
        )
    }

    single<UserAccountProvider> {
        KeycloakUserAccountProvider(
            userAccountFinder = get(),
            keycloakRealm = get(),
            keycloakClient = get(),
            accountStore = get()
        )
    }

    single<RegistrationHandler> {
        KeycloakRegistrationHandler(
            userAccountProvider = get(),
            mailer = get(),
            logger = LoggerFactory.getLogger("notifications")
        )
    }

    single(named("UsersSearchAdapter")) {
        PostgreSqlSearchAdapter(
            table = "accounts",
            sqlClient = get(),
            transformer = fun(row: Row): UserView {
                return UserView(
                    id = row.getUUID("id"),
                    email = row.getString("email"),
                    firstName = row.getString("first_name"),
                    lastName = row.getString("last_name"),
                    birthDateTime = row.getLocalDate("birth_date"),
                    gender = row.getString("gender"),
                    groups = row.getArrayOfStrings("group_list").toList(),
                    attributes = mapOf(
                        "orcId" to row.getString("orc_id"),
                        "institutionId" to row.getString("institution_id"),
                        "relatedInstitutionId" to row.getString("related_institution_id"),
                        "countryCode" to row.getString("country_code"),
                        "home_institution_id" to row.getString("home_institution_id"),
                        "nationality" to row.getString("nationality"),
                        "country_other_institution" to row.getString("country_other_institution")
                    ),
                    bannedAt = row.getLocalDateTime("banned_at"),
                    bannedWithReason = row.getString("banned_with_reason")
                )
            }
        )
    }
}
