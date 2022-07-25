package junit

import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.SqlClient
import org.junit.jupiter.api.extension.*
import org.testcontainers.utility.DockerImageName

internal class PostgresExtension : BeforeEachCallback, BeforeAllCallback, AfterEachCallback, AfterAllCallback,
    ParameterResolver {

    private companion object {
        const val container = "postgres:12.1"

        var databaseConfiguration = DatabaseConfiguration(
            host = "0.0.0.0",
            port = 5432,
            user = "elvis",
            password = "elvis",
            name = "elvis"
        )

        private val postgresImage: DockerImageName = DockerImageName.parse(container)

        val postgres: KGenericContainer = postgresImage.createContainer()
            .withEnv(
                mapOf(
                    "POSTGRES_USER" to databaseConfiguration.user,
                    "POSTGRES_PASSWORD" to databaseConfiguration.password,
                    "POSTGRES_DB" to databaseConfiguration.name
                )
            )
            .withExposedPorts(databaseConfiguration.port)
    }

    private var sqlClient: PgPool? = null

    override fun beforeAll(context: ExtensionContext) = postgres
        .start()
        .also {
            databaseConfiguration = databaseConfiguration.copy(
                host = postgres.containerIpAddress,
                port = postgres.firstMappedPort
            )

            DatabaseMigrator(databaseConfiguration).execute()
        }

    override fun afterAll(context: ExtensionContext) = postgres.stop()

    override fun beforeEach(context: ExtensionContext) {
        sqlClient = sqlClientFactory(databaseConfiguration)
    }

    override fun afterEach(context: ExtensionContext?) {
        sqlClient?.truncateDatabase().also {
            sqlClient = null
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type == PgPool::class.java || parameterContext.parameter.type == SqlClient::class.java

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any =
        sqlClient ?: error("DatabaseExtension is not initialized, check JUnit Extension import order")
}
