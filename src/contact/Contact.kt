package org.synthesis.contact

import org.synthesis.account.UserAccount
import org.synthesis.infrastructure.mailer.EmailAttachment
import org.synthesis.infrastructure.mailer.MailEnvelope
import org.synthesis.infrastructure.mailer.Mailer

data class ContactFormData(
    var message: String? = null,
    var subject: String
)

class ContactHandler(
    private val mailer: Mailer
) {

    suspend fun handle(contactFormData: ContactFormData, authUser: UserAccount, attachments: List<EmailAttachment>) {
        mailer.send(
            MailEnvelope(
                recipientEmail = authUser.email,
                recipientFullName = authUser.fullName,
                subject = contactFormData.subject,
                attachments = attachments,
                body = """
Message from: ${authUser.fullName.firstName} ${authUser.fullName.lastName}\n
Contact email: ${authUser.email}\n
Message: ${contactFormData.message}\n
""".trimIndent(),
            )
        )
    }
}
