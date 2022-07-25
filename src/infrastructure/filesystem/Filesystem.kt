package org.synthesis.infrastructure.filesystem

import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.future.await
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*

sealed class FilesystemException(message: String? = null) : Exception(message) {
    class InteractionsFailed(message: String?) : FilesystemException(message)
    class NotFound : FilesystemException()
    class BucketAlreadyExists : FilesystemException()
}

data class FileName(val value: String)
data class Folder(val value: String)

data class FileData(
    val name: FileName,
    val directory: Folder
)

interface Filesystem {

    /**
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun createDirectory(path: Folder)

    /**
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun copy(fromSource: File, to: FileData)

    /**
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun put(bytes: ByteArray, to: FileData)

    /**
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun put(content: String, to: FileData)

    /**
     * @throws [FilesystemException.NotFound]
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun read(file: FileData): ByteBuffer

    /**
     * @throws [FilesystemException.InteractionsFailed]
     */
    suspend fun remove(file: FileData)
}

class S3Filesystem(
    private val client: S3AsyncClient
) : Filesystem {

    override suspend fun createDirectory(path: Folder) {
        try {
            client.withCatch {
                val request = CreateBucketRequest.builder()
                    .bucket(path.value)
                    .build()

                createBucket(request).await()
            }
        } catch (e: FilesystemException.BucketAlreadyExists) {
            // not interests
        }
    }

    override suspend fun put(content: String, to: FileData) = uploadFile(AsyncRequestBody.fromString(content), to)
    override suspend fun copy(fromSource: File, to: FileData) = uploadFile(AsyncRequestBody.fromFile(fromSource), to)
    override suspend fun put(bytes: ByteArray, to: FileData) = uploadFile(AsyncRequestBody.fromBytes(bytes), to)

    override suspend fun read(file: FileData): ByteBuffer {
        client.withCatch {
            val request = GetObjectRequest.builder()
                .bucket(file.directory.value)
                .key(file.name.value)
                .build()

            return getObject(request, AsyncResponseTransformer.toBytes()).await().asByteBuffer()
        }
    }

    override suspend fun remove(file: FileData) {
        client.withCatch {
            val request = DeleteObjectRequest.builder()
                .bucket(file.directory.value)
                .key(file.name.value)
                .build()

            deleteObject(request).await()
        }
    }

    private suspend fun uploadFile(payload: AsyncRequestBody, to: FileData) {
        createDirectory(to.directory)

        client.withCatch {
            val request = PutObjectRequest.builder()
                .bucket(to.directory.value)
                .key(to.name.value)
                .build()

            client.putObject(request, payload).await()
        }
    }

    @Suppress("InstanceOfCheckForException")
    private inline fun <R> S3AsyncClient.withCatch(code: S3AsyncClient.() -> R): R = try {
        code()
    } catch (e: Throwable) {

        if (e.cause is NoSuchKeyException) {
            throw FilesystemException.NotFound()
        }

        if (e is BucketAlreadyOwnedByYouException) {
            throw FilesystemException.BucketAlreadyExists()
        }

        val cause = e.cause

        throw FilesystemException.InteractionsFailed(
            if (cause != null) cause.message else e.message
        )
    }
}
