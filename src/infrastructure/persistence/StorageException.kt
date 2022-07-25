package org.synthesis.infrastructure.persistence

import io.vertx.pgclient.PgException
import io.vertx.sqlclient.SqlClient

sealed class StorageException(message: String?) : Exception(message) {
    class UniqueConstraintViolationCheckFailed : StorageException(null)
    class InteractingFailed(withMessage: String) : StorageException(withMessage)
}

fun PgException.isUniqueConstraintFailed(): Boolean = code == "23505" || (message?.contains("duplicate key") ?: false)

fun PgException.adapt(): StorageException {
    if (isUniqueConstraintFailed()) {
        return StorageException.UniqueConstraintViolationCheckFailed()
    }

    return StorageException.InteractingFailed(message.toString())
}

/**
 * Adapt pq exceptions
 */
inline fun <R> SqlClient.withCatch(code: SqlClient.() -> R): R = try {
    code()
} catch (e: PgException) {
    throw e.adapt()
}
