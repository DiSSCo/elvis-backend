package org.synthesis.calls

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.ZipOutputStream
import org.koin.ktor.ext.inject
import org.synthesis.attachment.AttachmentId
import org.synthesis.attachment.AttachmentMetadata
import org.synthesis.attachment.AttachmentMimeType
import org.synthesis.attachment.fileNameWithExtension
import org.synthesis.auth.interceptor.authenticatedUser
import org.synthesis.auth.interceptor.withRole
import org.synthesis.calls.contract.CallCommand
import org.synthesis.calls.request.CallRequestId
import org.synthesis.calls.request.attachment.CallRequestAttachmentOwner
import org.synthesis.calls.request.comment.CallRequestCommentsHandler
import org.synthesis.calls.request.comment.CallRequestCommentsPresenter
import org.synthesis.calls.request.flow.CallRequestCommand
import org.synthesis.calls.request.flow.CallRequestFlowProxy
import org.synthesis.calls.request.ta.presenter.TaCallRequestPresenter
import org.synthesis.calls.request.ta.scoring.ScorePresenter
import org.synthesis.calls.request.va.attachmnent.VaCallRequestAttachmentPresenter
import org.synthesis.calls.request.va.attachmnent.VaCallRequestAttachmentStore
import org.synthesis.calls.request.va.presenter.VaCallRequestPresenter
import org.synthesis.calls.store.RequestTypeAllocator
import org.synthesis.comment.CommentFormat
import org.synthesis.formbuilder.FieldId
import org.synthesis.formbuilder.FieldValue
import org.synthesis.formbuilder.GroupId
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.addFile
import org.synthesis.infrastructure.ktor.*
import org.synthesis.institution.InstitutionId
import org.synthesis.institution.institutionId

/**
 * Routes:
 *
 *   - Create a new Call: POST /calls
 *   - Update Call: POST /{callId}/update
 *   - Delete Call: POST /{callId}/delete
 *   - Create new request: POST /call-requests
 *   - View request: GET /call-requests/{requestId}
 *   - Update request form value: POST /call-requests/{requestId}/set-field
 *   - Remove group value from request: POST /call-requests/{requestId}/delete-group
 *   - Add institution form to request: POST /call-requests/{requestId}/add-institution
 *   - Remove institution form from request: POST /call-requests/{requestId}/delete-institution
 *   - Submit request: POST /call-requests/{requestId}/submit
 *   - Remove request: POST /call-requests/{requestId}/delete
 *   - Close request: POST /call-requests/{requestId}/close
 *   - Approve institution details by coordinator: POST /call-requests/{requestId}/coordinator/approve
 *   - Cancel confirmation of the correctness of the entered data for the institute: POST /call-requests/{requestId}/coordinator/undo-approve
 *   - Receive information about CallRequest: GET /call-requests/{requestId}/coordinator
 *   - List of attachments on request for a specific institution form: GET /call-requests/{requestId}/attachments/{institutionId}
 *   - Add attachment(s) to request for a specific institution: POST /call-requests/{requestId}/attachments
 *   - Download attachment: GET /call-requests/{requestId}/attachments/{attachmentId}
 *   - Delete the specified attachment: POST /call-requests/{requestId}/attachments/{attachmentId}/remove
 *   - Add a new comment to thread, or reply to a comment: POST /call-requests/{requestId}/comments
 *   - Receive all comments for the specified application: GET /call-requests/{requestId}/comments
 *   - Receive all comments for the specified application (as PDF file): GET /call-requests/{requestId}/comments/download
 *   - Export request details to PDF:  GET /call-requests/{requestId}/export
 *   - Download archive containing all attachments and generated pdf file (with request overview): GET /call-requests/{requestId}/download
 */
@Suppress("LongMethod")
@InternalAPI
fun Route.callRoutes() {
    val callProvider by inject<CallProvider>()

    val callRequestFlowProxy by inject<CallRequestFlowProxy>()
    val vaCallRequestPresenter by inject<VaCallRequestPresenter>()
    val taCallRequestPresenter by inject<TaCallRequestPresenter>()
    val requestTypeAllocator by inject<RequestTypeAllocator>()
    val vaCallRequestAttachmentPresenter by inject<VaCallRequestAttachmentPresenter>()
    val vaCallRequestAttachmentStore by inject<VaCallRequestAttachmentStore>()
    val scorePresenter by inject<ScorePresenter>()

    val callRequestCommentsHandler by inject<CallRequestCommentsHandler>()
    val callRequestCommentsPresenter by inject<CallRequestCommentsPresenter>()

    route("/calls") {

        /**
         * Create a new Call.
         */
        withRole("call_create") {
            post {
                val createdCallId = callProvider.handle(
                    command = call.receiveValidated<CallCommand.Create>()
                )

                call.respondCreated("Call successful created", mapOf("id" to createdCallId.uuid))
            }
        }

        route("/{callId}") {

            /**
             * Update Call.
             */
            withRole("call_edit") {
                post("/update") {
                    val callId = call.callId()

                    callProvider.handle(
                        id = callId,
                        command = call.receiveValidated()
                    )

                    call.respondCreated("Call successful Updated", mapOf("id" to callId.uuid))
                }

                /**
                 * Delete Call.
                 */
                withRole("call_delete") {
                    post("/delete") {
                        callProvider.handle(
                            command = CallCommand.Delete(
                                id = call.callId()
                            )
                        )

                        call.respondSuccess()
                    }
                }
            }
        }
    }

    route("/call-requests") {
        /**
         * Create new request.
         */
        withRole("request_create") {
            post {
                val request = call.receiveValidated<CreateCallRequest>()
                val requestId = CallRequestId.next()

                callRequestFlowProxy.handle(
                    CallRequestCommand.CreateRequest(
                        callId = CallId(request.callId),
                        callRequestId = requestId,
                        user = authenticatedUser()
                    )
                )

                call.respondCreated("Call request successful created", CreateCallResponse(requestId.uuid))
            }
        }

        route("/{requestId}") {
            /**
             * View request.
             */
            withRole("request_view") {
                get {
                    val callRequestId = call.callRequestId()

                    when (requestTypeAllocator.allocate(callRequestId.uuid)) {
                        CallType.VA -> {
                            val request = vaCallRequestPresenter.find(call.callRequestId())
                                ?: throw IncorrectRequestParameters.create("requestId", "Request not found")

                            call.respond(request)
                        }

                        CallType.TA -> {
                            val request = taCallRequestPresenter.find(call.callRequestId())
                                ?: throw IncorrectRequestParameters.create("requestId", "Request not found")

                            call.respond(request)
                        }

                        else -> error("Unknown type")
                    }
                }
            }

            /**
             * Update request form value.
             */
            withRole("request_edit") {
                post("/set-field") {
                    val request = call.receiveValidated<SetFieldRequest>()
                    callRequestFlowProxy.handle(
                        CallRequestCommand.SetRequestFieldValue(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId(),
                            institutionId = createInstitutionId(request.institutionId),
                            fieldId = FieldId.fromString(request.fieldId),
                            fieldValue = request.value
                        )
                    )
                    call.respondSuccess()
                }
            }

            /**
             * Remove group value from request.
             */
            withRole("request_edit") {
                post("/delete-group") {
                    val request = call.receiveValidated<DeleteGroupRequest>()

                    callRequestFlowProxy.handle(
                        CallRequestCommand.DeleteFieldGroup(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId(),
                            institutionId = createInstitutionId(request.institutionId),
                            fieldGroupId = GroupId.fromString(request.groupId),
                        )
                    )

                    call.respondSuccess()
                }
            }

            /**
             * Add institution form to request.
             */
            withRole("request_edit") {
                post("/add-institution") {
                    val request = call.receiveValidated<AddInstitutionRequest>()

                    callRequestFlowProxy.handle(
                        CallRequestCommand.AddInstitutionForm(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId(),
                            institutionId = InstitutionId.fromString(request.institutionId)
                        )
                    )

                    call.respondSuccess()
                }
            }

            /**
             * Remove institution form from request.
             */
            withRole("request_edit") {
                post("/delete-institution") {
                    val request = call.receiveValidated<DeleteInstitutionRequest>()

                    callRequestFlowProxy.handle(
                        CallRequestCommand.DeleteInstitutionForm(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId(),
                            institutionId = InstitutionId.fromString(request.institutionId)
                        )
                    )

                    call.respondSuccess()
                }
            }

            /**
             * Submit request.
             */
            withRole("request_edit") {
                post("/submit") {

                    callRequestFlowProxy.handle(
                        CallRequestCommand.SubmitRequest(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId()
                        )
                    )

                    call.respondSuccess()
                }
            }

            /**
             * Remove request.
             */
            withRole("request_edit") {
                post("/delete") {

                    callRequestFlowProxy.handle(
                        CallRequestCommand.DeleteRequest(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId()
                        )
                    )

                    call.respondSuccess()
                }
            }

            /**
             * Close request.
             */
            withRole("request_edit") {
                post("/close") {

                    callRequestFlowProxy.handle(
                        CallRequestCommand.CloseRequest(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId()
                        )
                    )

                    call.respondSuccess()
                }
            }

            /**
             * Withdraw specified call request
             */
            withRole("request_withdraw") {
                post("/withdraw") {
                    callRequestFlowProxy.handle(
                        CallRequestCommand.WithdrawRequest(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId()
                        )
                    )

                    call.respondSuccess()
                }
            }

            /**
             * Approve institution details by coordinator.
             */
            withRole("request_approve") {
                post("/coordinator/approve") {
                    callRequestFlowProxy.handle(
                        CallRequestCommand.ApproveRequest(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId()
                        )
                    )

                    call.respondSuccess()
                }
            }

            /**
             * Cancel confirmation of the correctness of the entered data for the institute.
             */
            withRole("request_approve") {
                post("/coordinator/undo-approve") {
                    callRequestFlowProxy.handle(
                        CallRequestCommand.UndoRequestApprove(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId()
                        )
                    )

                    call.respondSuccess()
                }
            }

            withRole("request_score") {
                post("/scorer/create") {
                    val id = callRequestFlowProxy.handle(
                        CallRequestCommand.CreateScore(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId()
                        )
                    )

                    call.respondSuccess(mapOf("id" to id))
                }
            }

            withRole("request_score") {
                post("/scorer/{scoreFormId}/score") {
                    val request = call.receiveValidated<SetFieldScore>()

                    callRequestFlowProxy.handle(
                        CallRequestCommand.Score(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId(),
                            scoreForm = request.list,
                            scoreFormId = call.scoreFormId()
                        )
                    )

                    call.respondSuccess()
                }
            }

            withRole("request_score") {
                post("/scorer/{scoreFormId}/delete") {
                    callRequestFlowProxy.handle(
                        CallRequestCommand.DeleteScore(
                            callId = null,
                            user = authenticatedUser(),
                            callRequestId = call.callRequestId(),
                            scoreFormId = call.scoreFormId()
                        )
                    )

                    call.respondSuccess()
                }
            }

            withRole("request_scores_view") {
                get("/scorer/{scoreFormId}") {
                    val score = scorePresenter.find(call.scoreFormId())
                        ?: throw IncorrectRequestParameters.create("scoreFormId", "Score not found")

                    call.respond(score)
                }
            }

            /**
             * Receive information about CallRequest.
             *
             * @todo: remove me
             */
            withRole("request_view") {
                get("/coordinator") {
                    val request = vaCallRequestPresenter.find(call.callRequestId())
                        ?: throw IncorrectRequestParameters.create("requestId", "Request not found")

                    call.respond(request)
                }
            }

            route("/attachments/{institutionId}") {
                /**
                 * List of attachments on request for a specific institution form.
                 */
                withRole("request_attachment_view") {
                    get {
                        call.respondCollection(
                            vaCallRequestAttachmentPresenter.list(call.callRequestId(), call.institutionId())
                        )
                    }
                }

                /**
                 * Add attachment(s) to request for a specific institution.
                 * It is possible to add several attachments at once, but by agreement, one file is now added per request.
                 */
                withRole("request_attachment_add") {
                    post {

                        val currentUser = authenticatedUser()
                        val callRequestId = call.callRequestId()
                        val institutionId = call.institutionId()

                        val storedFiles: List<AttachmentId> = call.receiveFiles().map {
                            vaCallRequestAttachmentStore.add(
                                callRequestId = callRequestId,
                                institutionId = institutionId,
                                user = CallRequestAttachmentOwner(
                                    accountId = currentUser.id
                                ),
                                fileName = it.originalFileName,
                                payload = it.content.toByteArray(),
                                metadata = AttachmentMetadata(
                                    extension = it.extension,
                                    mimeType = AttachmentMimeType(
                                        base = it.contentType.contentType,
                                        subType = it.contentType.contentSubtype
                                    )
                                ),
                            )
                        }

                        call.respondCollection(storedFiles)
                    }
                }
            }

            route("/attachments/{attachmentId}") {
                /**
                 * Download attachment.
                 */
                withRole("request_attachment_view") {
                    get("/download") {
                        val attachment = vaCallRequestAttachmentPresenter
                            .download(call.callRequestAttachmentId())
                            ?: throw IncorrectRequestParameters.create(
                                field = "attachmentId",
                                message = "Attachment for request `${call.callRequestId()}` not found"
                            )

                        call.respondAttachment("${attachment.name}.${attachment.metadata.extension}") {
                            writeFully(attachment.payload)
                        }
                    }
                }

                /**
                 * Delete the specified attachment.
                 */
                withRole("request_attachment_remove") {
                    post("/remove") {
                        vaCallRequestAttachmentStore.remove(call.callRequestId(), call.callRequestAttachmentId())

                        call.respondSuccess()
                    }
                }
            }

            route("/comments") {
                /**
                 * Add a new comment to thread, or reply to a comment.
                 */
                withRole("request_comments_add") {
                    post {
                        val request = call.receiveValidated<AddCommentRequest>()

                        callRequestCommentsHandler.handle(
                            requestId = call.callRequestId(),
                            authorId = authenticatedUser().id,
                            body = request.body,
                            format = request.format,
                            replyTo = request.replyTo
                        )

                        call.respondSuccess()
                    }
                }

                /**
                 * Receive all comments for the specified application.
                 */
                withRole("request_comments_view") {
                    get {
                        call.respondSuccess(
                            callRequestCommentsPresenter.find(call.callRequestId())
                        )
                    }
                }

                /**
                 * Receive all comments for the specified application (as PDF file).
                 */
                withRole("request_comments_download") {
                    get("/download") {
                        val callRequestId = call.callRequestId()

                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            "attachment; filename=\"${callRequestId.uuid}_comments.pdf\""
                        )

                        call.respondOutputStream(ContentType.parse("application/pdf")) {
                            callRequestFlowProxy.commentExportHandle(
                                CallRequestCommand.ExportRequest(
                                    callId = null,
                                    callRequestId = callRequestId,
                                    user = authenticatedUser()
                                ),
                                to = this
                            )
                        }
                    }
                }
            }

            /**
             * Export request details to PDF.
             */
            withRole("request_export") {
                get("/export") {
                    val callRequestId = call.callRequestId()

                    call.respondAttachment("${callRequestId.uuid}.pdf") {
                        callRequestFlowProxy.overviewExportHandle(
                            CallRequestCommand.ExportRequest(
                                callId = null,
                                callRequestId = callRequestId,
                                user = authenticatedUser()
                            ),
                            to = toOutputStream()
                        )
                    }
                }
            }
            /**
             * Download archive containing all attachments and generated pdf file (with request overview).
             */
            withRole("request_download") {
                get("/download") {
                    val callRequestId = call.callRequestId()

                    /**
                     * Register of files added to the archive.
                     */
                    val addedAttachments: MutableMap<String, Int> = mutableMapOf()

                    call.respondAttachment("${callRequestId.uuid}.zip") {
                        ZipOutputStream(toOutputStream()).use { stream ->
                            /** Add attachments */
                            vaCallRequestAttachmentPresenter.list(callRequestId)
                                .mapNotNull { vaCallRequestAttachmentPresenter.download(it.id) }
                                .forEach { attachment ->
                                    stream.addFile(attachment.fileNameWithExtension(), addedAttachments) {
                                        stream.write(attachment.payload.moveToByteArray())
                                    }
                                }

                            /** Add generated PDF file */
                            stream.addFile("request_${callRequestId.uuid}.pdf", addedAttachments) {
                                ByteArrayOutputStream().use {
                                    callRequestFlowProxy.overviewExportHandle(
                                        CallRequestCommand.ExportRequest(
                                            callId = null,
                                            callRequestId = callRequestId,
                                            user = authenticatedUser()
                                        ),
                                        to = it
                                    )
                                    stream.write(it.toByteArray())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createInstitutionId(value: String?) = if (!value.isNullOrEmpty()) {
    InstitutionId.fromString(value)
} else {
    null
}

data class AddInstitutionRequest(
    val institutionId: String
)

data class DeleteInstitutionRequest(
    val institutionId: String
)

data class CreateCallRequest(
    val callId: UUID
)

data class CreateCallResponse(
    val id: UUID
)

data class SetFieldRequest(
    val institutionId: String?,
    val fieldId: String,
    val value: FieldValue?
)

data class SetFieldScore(
    val list: List<ScoreFields>
)

data class ScoreFields(
    val fieldId: String,
    val value: FieldValue?
)

data class DeleteGroupRequest(
    val institutionId: String?,
    val groupId: String
)

data class AddCommentRequest(
    val body: String,
    val format: CommentFormat = CommentFormat.TEXT,
    val replyTo: UUID? = null
)
