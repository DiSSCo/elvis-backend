package org.synthesis.environment

import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.slf4j.LoggerFactory

val environmentModule = module {

    single {
        DefaultUserImporter(
            registrationHandler = get()
        )
    }

    single(named("MigratorLogger")) {
        LoggerFactory.getLogger("migration")
    }

    single {
        UserMigrator(
            registrationHandler = get(),
            sqlClient = get(),
            realm = get(),
            logger = get(named("MigratorLogger"))
        )
    }

    single {
        ResourceMigrator(
            sqlClient = get(),
            keycloakClient = get(),
            logger = get(named("MigratorLogger"))
        )
    }

    single {
        LegacyContentMigrator(
            sqlClient = get(),
            logger = get(named("MigratorLogger"))
        )
    }

    single {
        GroupAdjustmentMigrator(
            sqlClient = get(),
            userAccountProvider = get()
        )
    }
}
