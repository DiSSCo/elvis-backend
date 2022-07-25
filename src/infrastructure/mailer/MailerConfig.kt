package org.synthesis.infrastructure.mailer

sealed class MailerConfig {
    data class Smtp(
        val host: String?,
        val port: Int?,
        val ssl: Boolean?,
        val username: String?,
        val password: String?,
        val starttls: Boolean?
    ) : MailerConfig()
}
