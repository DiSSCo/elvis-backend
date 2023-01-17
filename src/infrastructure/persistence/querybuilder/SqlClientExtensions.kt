package org.synthesis.infrastructure.persistence.querybuilder

import io.vertx.kotlin.coroutines.await
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.synthesis.infrastructure.persistence.*

/**
 * Executes the specified query.
 *
 * @throws [StorageException.InteractingFailed]
 * @throws [StorageException.UniqueConstraintViolationCheckFailed]
 */
suspend fun SqlClient.execute(compiledQuery: CompiledQuery): RowSet<Row> = withCatch {
    val preparedQuery = preparedQuery(compiledQuery.sql)

    preparedQuery.execute(Tuple.tuple(compiledQuery.statements)).await()
}

/**
 * Executes the specified query (in transaction context).
 *
 * @throws [StorageException.InteractingFailed]
 * @throws [StorageException.UniqueConstraintViolationCheckFailed]
 */
suspend fun PgPool.execute(compiledQuery: CompiledQuery): RowSet<Row> = withCatch {
    val connection = connection.await()

    try {
        val preparedQuery = connection.preparedQuery(compiledQuery.sql)

        preparedQuery.execute(Tuple.tuple(compiledQuery.statements)).await()
    } finally {
        connection.close()
    }
}

/**
 * Executes the specified query and returns one entity as a result
 *
 * @throws [StorageException.InteractingFailed]
 */
suspend fun SqlClient.fetchOne(compiledQuery: CompiledQuery): Row? = withCatch {
    val preparedQuery = preparedQuery(compiledQuery.sql)
    val resultSet = preparedQuery.execute(Tuple.tuple(compiledQuery.statements)).await()

    resultSet.firstOrNull()
}

/**
 * Executes the specified query and returns an array (as [Flow]) of entities as a result.
 *
 * @throws [StorageException.InteractingFailed]
 */
fun SqlClient.fetchAll(compiledQuery: CompiledQuery): Flow<Row> = flow {
    withCatch {
        val preparedQuery = preparedQuery(compiledQuery.sql)
        val resultSet = preparedQuery.execute(Tuple.tuple(compiledQuery.statements)).await()

        for (row in resultSet) {
            emit(row)
        }
    }
}

/**
 * Executes the code specified in the block within a transaction.
 *
 * @throws [StorageException.UniqueConstraintViolationCheckFailed]
 * @throws [StorageException.InteractingFailed]
 * @throws [Exception]
 */
suspend fun <R> PgPool.transaction(code: suspend SqlConnection.() -> R): R = withCatch {
    val connection = connection.await()
    val transaction = connection.begin().await()

    try {
        code(connection).also {
            transaction.commit().await()
        }
    } finally {
        connection.close()
    }
}
