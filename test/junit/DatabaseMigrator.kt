package junit

import org.flywaydb.core.Flyway

internal class DatabaseMigrator(
    configuration: DatabaseConfiguration
) {
    private val versionTableName = "migration_versions"
    private val migrationsDirectory = "/migrations/"

    private val flyway = Flyway
        .configure()
        .baselineOnMigrate(true)
        .outOfOrder(true)
        .dataSource(
            "jdbc:postgresql://${configuration.host}:${configuration.port}/${configuration.name}",
            configuration.user,
            configuration.password
        )

    fun execute() = flyway
        .table(versionTableName)
        .locations(migrationsDirectory)
        .load()
        .migrate()
}
