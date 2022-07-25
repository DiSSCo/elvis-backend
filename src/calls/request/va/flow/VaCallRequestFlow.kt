package org.synthesis.calls.request.va.flow

import java.lang.RuntimeException
import java.time.LocalDateTime
import org.slf4j.Logger
import org.synthesis.account.UserAccountId
import org.synthesis.account.UserFullName
import org.synthesis.calls.Call
import org.synthesis.calls.CallException
import org.synthesis.calls.VaCallFormFactory
import org.synthesis.calls.request.CallRequestException
import org.synthesis.calls.request.flow.CallRequestCommand
import org.synthesis.calls.request.flow.CallRequestFlow
import org.synthesis.calls.request.va.VaCallRequest
import org.synthesis.calls.request.va.store.VaCallRequestFinder
import org.synthesis.calls.request.va.store.VaCallRequestStore
import org.synthesis.formbuilder.DynamicForm
import org.synthesis.formbuilder.FormBuilderExceptions
import org.synthesis.infrastructure.mailer.MailEnvelope
import org.synthesis.infrastructure.mailer.Mailer
import org.synthesis.infrastructure.mailer.SendmailResult
import org.synthesis.infrastructure.persistence.StorageException
import org.synthesis.institution.InstitutionStore
import org.synthesis.institution.coordinator.CoordinatorAllocator
import org.synthesis.institution.coordinator.CoordinatorType
import org.synthesis.keycloak.KeycloakExceptions
import org.synthesis.keycloak.api.KeycloakClient
import org.synthesis.keycloak.api.Resource
import org.synthesis.keycloak.api.Scope

/**
 * @todo: reduce dependencies
 */
class VaCallRequestFlow(
    private val vaCallRequestFinder: VaCallRequestFinder,
    private val vaCallRequestStore: VaCallRequestStore,
    private val institutionStore: InstitutionStore,
    private val coordinatorAllocator: CoordinatorAllocator,
    private val mailer: Mailer,
    private val keycloakClient: KeycloakClient,
    private val logger: Logger
) : CallRequestFlow {

    private val scope = Scope("VaCall Request")

    override suspend fun handle(call: Call, command: CallRequestCommand): Any = when (command) {
        is CallRequestCommand.CreateRequest -> createRequest(call, command)
        is CallRequestCommand.SetRequestFieldValue -> setRequestFieldValue(command)
        is CallRequestCommand.DeleteFieldGroup -> removeRequestFieldGroup(command)
        is CallRequestCommand.AddInstitutionForm -> addInstitutionForm(command)
        is CallRequestCommand.DeleteInstitutionForm -> removeInstitutionForm(command)
        is CallRequestCommand.SubmitRequest -> submitRequest(command)
        is CallRequestCommand.DeleteRequest -> deleteRequest(command)
        is CallRequestCommand.CloseRequest -> closeRequest(command)
        is CallRequestCommand.ApproveRequest -> approveRequest(command)
        is CallRequestCommand.UndoRequestApprove -> undoRequestApprove(command)
        is CallRequestCommand.WithdrawRequest -> withdrawRequest(command)
        else -> error("Unsupported command for VaCallRequest flow: ${command::class.java}")
    }

    /**
     * Withdraw the call request (performed by the administrator).
     *
     * @throws [CallException.CallRequestNotFound]
     * @throws [StorageException.InteractingFailed]
     */
    private suspend fun withdrawRequest(command: CallRequestCommand.WithdrawRequest): Unit = try {
        logger.info("Withdraw request `${command.callRequestId}` by `${command.user.id}`")

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)

        callRequest.withdrawRequest().also {
            vaCallRequestStore.save(callRequest)
        }

        logger.info("Request `${command.callRequestId}` successfully withdrawn by `${command.user.id}`")
    } catch (e: Exception) {
        logger.error("Withdrawal request `${command.callRequestId}` failed with message: ${e.message}", e)

        throw e
    }

    /**
     * Cancel confirmation of the correctness of the entered data for the institute.
     *
     * @throws [CallException.CallRequestNotFound]
     * @throws [CallRequestException.CantFindAssociatedInstitute] Cant find associated institute for VaCoordinator.
     * @throws [StorageException.InteractingFailed]
     */
    private suspend fun undoRequestApprove(command: CallRequestCommand.UndoRequestApprove): Unit = try {
        logger.info("Undo approving request `${command.callRequestId}` by `${command.user.id}`")

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)

        callRequest.undoApproveByVACoordinator(command.user.id).also {
            vaCallRequestStore.save(callRequest)
        }

        logger.info("Undo approving request `${command.callRequestId}` completed")
    } catch (e: Exception) {
        logger.error("Error undo approving request `${command.callRequestId}`: ${e.message}", e)

        throw e
    }

    /**
     * Approve institution details by coordinator.
     *
     * @throws [CallException.CallRequestNotFound]
     * @throws [CallRequestException.AttemptToApproveNotSubmittedRequest] You can approve only the request that was sent
     *                                                                  for consideration, or is already under
     *                                                                  consideration.
     * @throws [CallRequestException.CantFindAssociatedInstitute] Cant find associated institute for VaCoordinator.
     * @throws [StorageException.InteractingFailed]
     */
    private suspend fun approveRequest(command: CallRequestCommand.ApproveRequest): Unit = try {
        logger.info("Approving request `${command.callRequestId}` by `${command.user.id}`")

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)

        callRequest.approveByVACoordinator(command.user.id).also {
            vaCallRequestStore.save(callRequest)
        }

        logger.info("Request `${command.callRequestId}` approved")
    } catch (e: Exception) {
        logger.error("Approving request `${command.callRequestId}` failed with message: ${e.message}", e)

        throw e
    }

    /**
     * Close call request.
     *
     * @throws [CallException.CallRequestNotFound]
     * @throws [CallRequestException.AttemptToCloseUnapprovedRequest] You can only close a request confirmed by the
     *                                                              coordinators.
     * @throws [StorageException.InteractingFailed]
     */
    private suspend fun closeRequest(command: CallRequestCommand.CloseRequest): Unit = try {
        logger.info("Closing request `${command.callRequestId}` by `${command.user.id}`")

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)

        callRequest.close().also {
            vaCallRequestStore.save(callRequest)
        }

        logger.info("Request `${command.callRequestId}` successful closed")
    } catch (e: Exception) {
        logger.info("Closing request `${command.callRequestId}` failed with message: ${e.message}", e)

        throw e
    }

    /**
     * Delete call request (by admin).
     *
     * @throws [CallException.CallRequestNotFound]
     * @throws [CallRequestException.UnableToDeleteRequest] Call request can be deleted only in DRAFT status.
     */
    private suspend fun deleteRequest(command: CallRequestCommand.DeleteRequest): Unit = try {
        logger.info("Deleting request `${command.callRequestId}` by `${command.user.id}`")

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)

        callRequest.delete().also {
            vaCallRequestStore.save(callRequest)
        }

        logger.info("Request `${command.callRequestId}` successful deleted")
    } catch (e: Exception) {
        logger.info("Deleting request `${command.callRequestId}` failed with message: ${e.message}", e)

        throw e
    }

    /**
     * Submit call request.
     *
     * @throws [CallException.CallRequestNotFound]
     * @throws [CallRequestException.UnableToSendRequest] Submit is possible only with the status of "draft".
     * @throws [CallRequestException.NoInstitutionAdded] Unable to submit form to which no institute has been added.
     * @throws [StorageException.InteractingFailed]
     */
    private suspend fun submitRequest(command: CallRequestCommand.SubmitRequest): Unit = try {
        logger.info("Submitting request `${command.callRequestId}` by `${command.user.id}`")

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)

        callRequest.submit()

        vaCallRequestStore.save(callRequest).also {
            logger.info("Request `${command.callRequestId}` successful submitted. Notify coordinators")

            /** We must send notification to each added coordinator */
            callRequest.content().institutions.forEach {
                /** Cant be NULL */
                val vaCoordinator = coordinatorAllocator.find(it.value.coordinatorId()) ?: throw RuntimeException()
                val institution = institutionStore.findById(it.value.institutionId()) ?: throw RuntimeException()

                val result = mailer.send(
                    MailEnvelope(
                        recipientEmail = vaCoordinator.email,
                        recipientFullName = UserFullName(
                            firstName = vaCoordinator.firstName,
                            lastName = vaCoordinator.lastName
                        ),
                        subject = "A new VA request for your Institution has been created in ELViS",
                        body = "In the European Loans and Visits System (ELViS) a new VA request for the institution " +
                                "“${institution.title()}” for which you are VA Coordinator has been submitted " +
                                "Please go to: {frontendUrl}/requests/va/${callRequest.id()} to evaluate this request."
                    )
                )

                when (result) {
                    is SendmailResult.Success -> logger.info(
                        """Email notification for coordinator `${vaCoordinator.email}` (`${institution.id()}`) 
                           |successful sent (for request `${command.callRequestId}`)
                        """.trimMargin()
                    )
                    is SendmailResult.Failed -> logger.error(
                        """Email notification send failed for coordinator `${vaCoordinator.email}` (`${institution.id()}`) 
                            |with message: ${result.cause.message}
                        """.trimMargin(),
                        result.cause
                    )
                }
            }
        }
    } catch (e: Exception) {
        logger.error("Request `${command.callRequestId}` submitting failed with message: ${e.message}", e)

        throw e
    }

    /**
     * Add form for institution details.
     *
     * @throws [CallException.CallRequestNotFound]
     * @throws [CallRequestException.InstitutionAlreadyAdded]
     * @throws [CallRequestException.CantFindAssociatedCoordinator]
     * @throws [StorageException.InteractingFailed]
     */
    private suspend fun addInstitutionForm(command: CallRequestCommand.AddInstitutionForm): Unit = try {
        logger.info(
            """Adding a form for an institute `${command.institutionId}` to request `${command.callRequestId}` 
                |by `${command.user.id}`
            """.trimMargin()
        )

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)

        val vaCoordinator = coordinatorAllocator.allocate(command.institutionId, CoordinatorType.VA)
            ?: throw CallRequestException.CantFindAssociatedCoordinator(command.institutionId)

        callRequest.addInstitution(command.institutionId, UserAccountId(vaCoordinator.id)).also {
            vaCallRequestStore.save(callRequest)
        }

        logger.info(
            """Form for institute `${command.institutionId}` has been successfully added to 
                |request `${command.callRequestId}`
            """.trimMargin()
        )
    } catch (e: Exception) {
        logger.error(
            """Error adding form for institute `${command.institutionId}` to request `${command.callRequestId}`: 
                |${e.message}
            """.trimMargin(),
            e
        )

        throw e
    }

    /**
     * Remove institution form.
     *
     * @throws [StorageException.InteractingFailed]
     */
    private suspend fun removeInstitutionForm(command: CallRequestCommand.DeleteInstitutionForm): Unit = try {
        logger.info(
            """Removal of the form for the institute `${command.institutionId}` on request `${command.callRequestId}` 
                |by `${command.user.id}`
            """.trimMargin()
        )

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)

        callRequest.removeInstitution(command.institutionId).also {
            vaCallRequestStore.save(callRequest)
        }

        logger.info(
            """Form for institute `${command.institutionId}` has been successfully removed from request 
                |`${command.callRequestId}`
            """.trimMargin()
        )
    } catch (e: Exception) {
        logger.error(
            """Error deleting form for institute `${command.institutionId}` on request `${command.callRequestId}`: 
                |${e.message}
            """.trimMargin(),
            e
        )

        throw e
    }

    /**
     * Remove field group from general/institution form.
     *
     * @throws [CallException.CallRequestNotFound]
     * @throws [StorageException.InteractingFailed]
     */
    private suspend fun removeRequestFieldGroup(command: CallRequestCommand.DeleteFieldGroup): Unit = try {
        logger.info(
            "Removing a group `${command.fieldGroupId}` from request `${command.callRequestId}` by `${command.user.id}`"
        )

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)
        val institutionId = command.institutionId

        if (institutionId != null) {
            callRequest.deleteFieldGroup(command.fieldGroupId, institutionId)
        } else {
            callRequest.deleteFieldGroup(command.fieldGroupId)
        }

        vaCallRequestStore.save(callRequest)

        logger.info("Field group `${command.fieldGroupId}` successfully removed from request `${command.callRequestId}`")
    } catch (e: Exception) {
        logger.error(
            """Error while deleting field group `${command.fieldGroupId}` from request `${command.callRequestId}`: 
                |${e.message}
            """.trimMargin(),
            e
        )

        throw e
    }

    /**
     * Set/update general/institution form value.
     *
     * @throws [CallException.CallRequestNotFound]
     * @throws [CallRequestException.InstitutionNotAdded]
     * @throws [FormBuilderExceptions.FieldNotFound]
     * @throws [FormBuilderExceptions.FieldTypeMismatch]
     * @throws [StorageException.InteractingFailed]
     */
    private suspend fun setRequestFieldValue(command: CallRequestCommand.SetRequestFieldValue): Unit = try {
        logger.info(
            "Updating a field `${command.fieldId}` value on request `${command.callRequestId}` by `${command.user.id}`"
        )

        val callRequest = vaCallRequestFinder.find(command.callRequestId)
            ?: throw CallException.CallRequestNotFound(command.callRequestId)
        val institutionId = command.institutionId

        if (institutionId != null) {
            callRequest.setFieldValue(command.fieldId, institutionId, command.fieldValue)
        } else {
            callRequest.setFieldValue(command.fieldId, command.fieldValue)
        }

        vaCallRequestStore.save(callRequest)
    } catch (e: Exception) {
        logger.error(
            "Error while updating field `${command.fieldId}` value on request `${command.callRequestId}` by `${command.user.id}`: ${e.message}",
            e
        )
    }

    /**
     * @throws [CallException.CallRequestAlreadyAdded]
     * @throws [CallException.CallClosedForRequest]
     * @throws [StorageException.InteractingFailed]
     * @throws [KeycloakExceptions.OperationFailed]
     */
    private suspend fun createRequest(call: Call, command: CallRequestCommand.CreateRequest): Unit = try {
        if (!call.opened()) {
            throw CallException.CallClosedForRequest(call.id())
        }

        keycloakClient.create(
            Resource(
                id = command.callRequestId.uuid.toString(),
                name = call.title(),
                type = "VaCall Request",
                scopes = listOf(scope),
                attributes = mapOf(
                    "id" to command.callRequestId.uuid.toString(),
                    "author_id" to command.user.id.uuid.toString()
                )
            )
        ).also {
            vaCallRequestStore.add(
                VaCallRequest(
                    id = command.callRequestId,
                    callId = command.callId,
                    authorId = command.user.id,
                    createdAt = LocalDateTime.now(),
                    content = VaCallRequest.ContentForm(
                        general = DynamicForm.create(VaCallFormFactory.general())
                    ),
                    status = VaCallRequest.Status.DRAFT,
                    deletedAt = null,
                    resourceId = it
                )
            )
        }

        Unit
    } catch (e: StorageException.UniqueConstraintViolationCheckFailed) {
        throw CallException.CallRequestAlreadyAdded(command.callRequestId)
    } catch (e: KeycloakExceptions.EntityAlreadyExists) {
        throw CallException.CallRequestAlreadyAdded(command.callRequestId)
    }
}
