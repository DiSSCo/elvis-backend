package org.synthesis.infrastructure.mailer

import io.ktor.utils.io.*

data class EmailAttachment(
    val name: String?,
    val content: ByteReadChannel,
    val type: String?
)
