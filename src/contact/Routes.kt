@file:Suppress("BlockingMethodInNonBlockingContext")

package org.synthesis.contact

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import org.koin.ktor.ext.inject
import org.synthesis.auth.interceptor.authenticatedUser
import org.synthesis.auth.ktor.withRole
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.respondSuccess
import org.synthesis.infrastructure.mailer.EmailAttachment
import kotlin.collections.set

@Suppress("LongMethod")
fun Route.contactRoutes() =
    authenticate {
        withRole("authificated") {}
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
