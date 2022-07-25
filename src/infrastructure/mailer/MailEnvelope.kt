package org.synthesis.infrastructure.mailer

import org.synthesis.account.UserFullName

data class MailEnvelope(
    val subject: String,
    val body: String,
    val recipientEmail: String,
    val recipientFullName: UserFullName,
    val attachments: List<EmailAttachment> = listOf()
)
