package org.synthesis.environment

import kotlinx.coroutines.runBlocking
import org.koin.core.Koin
import org.koin.core.KoinComponent
import org.synthesis.infrastructure.persistence.Migrator

object EnvironmentSetup : KoinComponent {
    enum class Stage {
        IMPORT_DEV_USERS,
        MIGRATE_USERS,
        MIGRATE_RESOURCES,
        MIGRATE_CONTENT,
        USER_GROUP_ADJUSTMENT
    }

    fun prepare() = runBlocking {
        val koin = getKoin()
        val migrator = koin.get<Migrator>()

        migrator.migrate()

        koin.stages().forEach {
            when (it) {
                Stage.IMPORT_DEV_USERS -> koin.get<DefaultUserImporter>().execute()
                Stage.MIGRATE_RESOURCES -> koin.get<ResourceMigrator>().execute()
                Stage.MIGRATE_USERS -> koin.get<UserMigrator>().execute()
                Stage.MIGRATE_CONTENT -> koin.get<LegacyContentMigrator>().execute()
                Stage.USER_GROUP_ADJUSTMENT -> koin.get<GroupAdjustmentMigrator>().execute()
            }
        }
    }

    private fun Koin.stages() = getProperty("APP_ROLE")
        ?.split(" ")
        ?.map { Stage.valueOf(it.toUpperCase()) }
        ?.toSet()
        ?: emptySet()
}
