package org.synthesis.infrastructure.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.utils.io.*
import java.io.File

data class UploadedFile(
    val originalFileName: String,
    val extension: String,
    val contentType: ContentType,
    val content: ByteReadChannel
)

/**
 * Receive files from multipart request.
 *
 * @throws [FileUploadFailed.IncorrectAttachmentName]
 * @throws [FileUploadFailed.IncorrectAttachmentExtension]
 * @throws [FileUploadFailed.UnableToDetectMediaType]
 */
suspend fun ApplicationCall.receiveFiles(): List<UploadedFile> {

    val files: MutableList<UploadedFile> = mutableListOf()

    receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FileItem -> files.add(
                UploadedFile(
                    originalFileName = part.originalFileName?.let { File(it).nameWithoutExtension }
                        ?: throw FileUploadFailed.IncorrectAttachmentName(),
                    extension = part.originalFileName?.let { File(it).extension }
                        ?: throw FileUploadFailed.IncorrectAttachmentExtension(),
                    contentType = part.contentType ?: throw FileUploadFailed.UnableToDetectMediaType(),
                    content = ByteReadChannel(part.streamProvider().readBytes())
                )
            )
            else -> Unit
        }
    }

    return files
}

sealed class FileUploadFailed(message: String) : Exception(message) {
    class IncorrectAttachmentName : FileUploadFailed("Attachment name must be specified")
    class IncorrectAttachmentExtension : FileUploadFailed("Attachment extension must be specified")
    class UnableToDetectMediaType : FileUploadFailed("Unidentified attachment content type")
}
