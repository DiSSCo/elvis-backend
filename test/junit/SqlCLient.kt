package junit

import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import kotlinx.coroutines.runBlocking

internal fun sqlClientFactory(configuration: DatabaseConfiguration): PgPool {

    val connectionOptions = PgConnectOptions().apply {
        port = configuration.port
        host = configuration.host
        database = configuration.name
        user = configuration.user
        password = configuration.password
    }

    return PgPool.pool(
        connectionOptions,
        PoolOptions()
    )
}

internal fun PgPool.truncateDatabase() = runBlocking {
    val preparedQuery = preparedQuery(
        """
            SELECT tablename FROM pg_catalog.pg_tables 
            WHERE schemaname != 'pg_catalog' AND schemaname != 'information_schema';
        """.trimIndent()
    )

    val rowSet = preparedQuery.execute().await()

    for (row in rowSet) {

        preparedQuery("TRUNCATE TABLE ${row.getString("tablename")} CASCADE").execute().await()
    }

    Unit
}
