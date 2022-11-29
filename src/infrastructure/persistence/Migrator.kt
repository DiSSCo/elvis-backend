package org.synthesis.infrastructure.persistence

import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.exception.FlywaySqlException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Migrator {
    fun migrate()
}

class FlyWayMigrator(
    private val configuration: StorageConfiguration,
    private val historyTableName: String = "migration_versions",
    private val migrationsDirectory: String = "/migrations/",
    private val logger: Logger = LoggerFactory.getLogger("app")
) : Migrator {

    override fun migrate(): Unit = try {
        execute()
    } catch (e: FlywaySqlException) {
        val message = e.message ?: ""
        logger.error(message)
        if (message.contains("Unable to obtain connection")) {
            logger.info("Retrying database connection")
            Thread.sleep(500)

            migrate()
        } else {
            throw e
        }
    }

    private fun execute() {
        flyway()
            .table(historyTableName)
            .locations(migrationsDirectory)
            .load()
            .migrate()
    }

    private fun flyway() = Flyway
        .configure()
        .baselineOnMigrate(true)
        .outOfOrder(true)
        .dataSource(configuration.jdbcDSN(), configuration.username, configuration.password)
}
