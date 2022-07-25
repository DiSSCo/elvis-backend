package org.synthesis.infrastructure.persistence

data class StorageConfiguration(
    val driver: String = "postgresql",
    val host: String,
    val port: Int,
    val database: String,
    val username: String? = null,
    val password: String? = null
)

fun StorageConfiguration.jdbcDSN(): String = "jdbc:$driver://$host:$port/$database"
