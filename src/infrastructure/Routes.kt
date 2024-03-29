package org.synthesis.infrastructure

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.synthesis.account.UserFullName
import org.synthesis.auth.ktor.withRole
import org.synthesis.infrastructure.ktor.receiveFromParameters
import org.synthesis.infrastructure.ktor.respondSuccess
import org.synthesis.infrastructure.mailer.MailEnvelope
import org.synthesis.infrastructure.mailer.Mailer
import org.synthesis.infrastructure.mailer.SendmailResult

fun Route.infrastructure() {

    val mailer by inject<Mailer>()

    authenticate {
        withRole("settings_edit") {}

        post("/debug/mailer/{email}") {
            val result = mailer.send(
                MailEnvelope(
                    subject = "Elvis debug email",
                    body = "Some test message",
                    recipientEmail = call.receiveFromParameters("email"),
                    recipientFullName = UserFullName(
                        firstName = "Maksim",
                        lastName = "Masiukevich"
                    )
                )
            )

            call.respondSuccess(
                mapOf("result" to (result !is SendmailResult.Failed))
            )
        }
    }
}
