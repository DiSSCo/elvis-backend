@file:Suppress("BlockingMethodInNonBlockingContext")

package org.synthesis.contact

import io.ktor.server.application.call
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.utils.io.*
import org.koin.ktor.ext.inject
import org.synthesis.auth.interceptor.authenticatedUser
import org.synthesis.auth.interceptor.withRole
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.respondSuccess
import org.synthesis.infrastructure.mailer.EmailAttachment

@Suppress("LongMethod")
fun Route.contactRoutes() = withRole("authificated") {
    val contactHandler by inject<ContactHandler>()

    post("/contact") {
        val multipart = call.receiveMultipart()
        val attachments: MutableList<EmailAttachment> = mutableListOf()
        val formValues: MutableMap<String, String> = mutableMapOf()

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> attachments.add(
                    EmailAttachment(
                        name = part.originalFileName,
                        content = ByteReadChannel(part.streamProvider().readBytes()),
                        type = part.contentType?.contentType
                    )
                )
                is PartData.FormItem -> {
                    val key = part.name ?: throw IncorrectRequestParameters.create(
                        "form_data",
                        "Form field (with value `${part.value}`) name must be specified"
                    )

                    formValues[key] = part.value
                }
                else -> Unit
            }
        }

        val contactFormData = ContactFormData(
            subject = formValues["subject"] ?: throw IncorrectRequestParameters.create(
                "subject",
                "Message subject must be specified"
            ),
            message = formValues["message"] ?: ""
        )

        contactHandler.handle(
            contactFormData = contactFormData,
            authUser = authenticatedUser(),
            attachments = attachments
        )

        call.respondSuccess()
    }
}
