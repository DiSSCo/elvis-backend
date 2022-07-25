package org.synthesis.infrastructure.mailer

import io.ktor.utils.io.*
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.mail.*
import io.vertx.ext.mail.impl.MailAttachmentImpl
import io.vertx.kotlin.coroutines.await
import org.slf4j.Logger
import org.synthesis.account.UserFullName

sealed class SendmailResult {

    data class Success(
        val recipientEmail: String,
        val recipientFullName: UserFullName
    ) : SendmailResult()

    data class Failed(
        val recipientEmail: String,
        val recipientFullName: UserFullName,
        val cause: Throwable
    ) : SendmailResult()
}

interface Mailer {
    suspend fun send(envelope: MailEnvelope): SendmailResult
}

data class EmailTemplateOptions(
    val frontendUrl: String,
    val fromEmail: String,
    val fromName: String,
    val supportEmail: String,
    val templatePath: String
)

fun Mailer.buildBody(envelope: MailEnvelope, templateOptions: EmailTemplateOptions): String = javaClass
    .getResource(templateOptions.templatePath)
    .readText()
    .replace("{messageBody}", envelope.body)
    .replace("{messageSubject}", envelope.subject)
    .replace("{frontendUrl}", templateOptions.frontendUrl)
    .replace("{supportEmail}", templateOptions.supportEmail)
    .replace("{recipient}", envelope.recipientFullName.toString())

class VertexMailer(
    private val config: MailerConfig.Smtp,
    private val templateOptions: EmailTemplateOptions
) : Mailer {

    private val client: MailClient by lazy {
        MailClient.create(
            Vertx.vertx(),
            MailConfig().apply {
                hostname = config.host ?: hostname
                port = config.port ?: port
                username = config.username ?: username
                password = config.password ?: password
                isSsl = config.ssl ?: isSsl
                isTrustAll = true
                starttls = when (config.starttls) {
                    true -> StartTLSOptions.REQUIRED
                    false -> StartTLSOptions.DISABLED
                    else -> StartTLSOptions.OPTIONAL
                }
                if (config.username != null && config.password != null) {
                    login = LoginOption.REQUIRED
                }
            }
        )
    }

    override suspend fun send(envelope: MailEnvelope) = try {
        val message = MailMessage().apply {
            from = "${templateOptions.fromEmail} (${templateOptions.fromName})"
            to = listOf(envelope.recipientEmail)
            subject = envelope.subject
            html = buildBody(envelope, templateOptions)
        }

        if (envelope.attachments.isNotEmpty()) {
            message.attachment = envelope.attachments.map {
                val buffer = it.content.readToBuffer()

                MailAttachmentImpl().apply {
                    data = buffer
                    contentType = it.type
                    name = it.name
                }
            }
        }

        client.sendMail(message).await()

        SendmailResult.Success(
            recipientEmail = envelope.recipientEmail,
            recipientFullName = envelope.recipientFullName
        )
    } catch (e: Throwable) {
        SendmailResult.Failed(
            recipientEmail = envelope.recipientEmail,
            recipientFullName = envelope.recipientFullName,
            cause = e
        )
    }

    private suspend fun ByteReadChannel.readToBuffer(to: Buffer = Buffer.buffer(), size: Int = 2048): Buffer {
        val array = ByteArray(size)

        while (!isClosedForRead) {
            read {
                val remaining = if (it.remaining() > size) size else it.remaining()

                it.get(array, 0, remaining)
                to.appendBytes(array, 0, remaining)
            }
        }

        return to
    }
}

class ConsoleMailer(
    private val templateOptions: EmailTemplateOptions,
    private val logger: Logger
) : Mailer {

    override suspend fun send(envelope: MailEnvelope): SendmailResult {
        val messageBody = buildBody(envelope, templateOptions)

        logger.info(
            "Email: sent to ${envelope.recipientEmail} with content: $messageBody and attachment ${envelope.attachments}"
        )

        return SendmailResult.Success(
            recipientEmail = envelope.recipientEmail,
            recipientFullName = envelope.recipientFullName
        )
    }
}
