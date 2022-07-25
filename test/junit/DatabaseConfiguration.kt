package junit

internal data class DatabaseConfiguration(
    val user: String,
    val password: String,
    val name: String,
    val host: String,
    val port: Int
)