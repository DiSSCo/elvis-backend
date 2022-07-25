package org.synthesis.calls.request.va

import java.time.LocalDateTime
import java.util.*
import org.synthesis.account.UserAccountId
import org.synthesis.calls.CallId
import org.synthesis.calls.CallType
import org.synthesis.calls.VaCallFormFactory
import org.synthesis.calls.request.CallRequest
import org.synthesis.calls.request.CallRequestException
import org.synthesis.calls.request.CallRequestId
import org.synthesis.country.Country
import org.synthesis.formbuilder.*
import org.synthesis.institution.InstitutionId
import org.synthesis.keycloak.api.KeycloakResourceId

class VaCallRequest(
    private val id: CallRequestId,
    private val callId: CallId,
    private val authorId: UserAccountId,
    private val createdAt: LocalDateTime,
    private val content: ContentForm,
    private var status: Status = Status.DRAFT,
    private var deletedAt: LocalDateTime? = null,
    private val resourceId: KeycloakResourceId? = null
) : CallRequest {
    enum class Status { DRAFT, SUBMITTED, APPROVED, CLOSED, BEING_HANDLED, WITHDRAWN }

    data class ContentForm(
        val general: DynamicForm,
        val institutions: MutableMap<InstitutionId, InstitutionForm> = mutableMapOf()
    )

    /**
     * Form to be completed for each added institution.
     */
    class InstitutionForm(
        private val id: UUID,
        private val institutionId: InstitutionId,
        private val coordinatorId: UserAccountId,
        private val form: DynamicForm,
        private var status: Status,
        private var deletedAt: LocalDateTime?
    ) {
        fun id(): UUID = id

        fun institutionId(): InstitutionId = institutionId

        fun coordinatorId(): UserAccountId = coordinatorId

        fun content(): DynamicForm = form

        fun status(): Status = status

        fun deletedAt(): LocalDateTime? = deletedAt

        /**
         * Mark institution form as deleted.
         */
        fun remove() {
            if (deletedAt == null) {
                deletedAt = LocalDateTime.now()
            }
        }

        /**
         * Confirm the correctness of the entered data (by coordinator).
         *
         * @throws CallRequestException.RequestNotApproved
         */
        fun approve() = changeStatus(Status.APPROVED)

        /**
         * Cancel confirmation of the correctness of the filled data (by coordinator).
         */
        fun undoApprove() {
            if (status != Status.APPROVED) {
                throw CallRequestException.RequestNotApproved()
            }

            changeStatus(Status.SUBMITTED)
        }

        private fun changeStatus(new: Status) {
            status = new
        }
    }

    override fun id(): CallRequestId = id

    override fun callId(): CallId = callId

    override fun callType(): CallType = CallType.VA

    override fun createdAt(): LocalDateTime = createdAt

    override fun deletedAt(): LocalDateTime? = deletedAt

    override fun authorId(): UserAccountId = authorId

    override fun title(): String? = generalFormValue(FieldId.fromString("subject"))

    override fun description(): String? = generalFormValue(FieldId.fromString("abstract"))

    /**
     * Va CallRequest doesnt depends from a country.
     */
    override fun country(): Country? = null

    fun resourceId(): KeycloakResourceId? = resourceId

    fun status(): Status = status

    fun content(): ContentForm = content

    /**
     * Add form for institution details.
     *
     * @throws [CallRequestException.InstitutionAlreadyAdded]
     */
    fun addInstitution(id: InstitutionId, coordinatorId: UserAccountId) {
        if (content.institutions.containsKey(id)) {
            throw CallRequestException.InstitutionAlreadyAdded(id)
        }

        content.institutions[id] = InstitutionForm(
            id = UUID.randomUUID(),
            institutionId = id,
            coordinatorId = coordinatorId,
            form = DynamicForm.create(VaCallFormFactory.institution()),
            status = Status.SUBMITTED,
            deletedAt = null
        )
    }

    /**
     * Remove institution form.
     * Returns the identifier of the coordinator responsible for this institution.
     */
    fun removeInstitution(institutionId: InstitutionId): UserAccountId? {
        val institutionForm = content.institutions[institutionId] ?: return null

        institutionForm.remove()

        return institutionForm.coordinatorId()
    }

    /**
     * Set/update general form field value.
     *
     * @throws [FormBuilderExceptions.FieldNotFound]
     * @throws [FormBuilderExceptions.FieldTypeMismatch]
    */
    fun setFieldValue(fieldId: FieldId, value: FieldValue?) =
        setValueToGeneralForm(fieldId, value)

    /**
     * Set/update institution form field value.
     *
     * @throws [CallRequestException.InstitutionNotAdded]
     * @throws [FormBuilderExceptions.FieldNotFound]
     * @throws [FormBuilderExceptions.FieldTypeMismatch]
     */
    fun setFieldValue(fieldId: FieldId, institutionId: InstitutionId, value: FieldValue?) =
        setValueToInstitutionForm(institutionId, fieldId, value)

    /**
     * Delete field group in general form.
     */
    fun deleteFieldGroup(id: GroupId) = content.general.deleteGroup(id)

    /**
     * Delete field group in institution form.
     */
    fun deleteFieldGroup(id: GroupId, institutionId: InstitutionId) = content.institutions[institutionId]
        ?.content()
        ?.deleteGroup(id)

    /**
     * Submit call request.
     * The form is submitted by the request author. After submitting the form, confirmation is expected from the
     * coordinators for each of the added institutions.
     *
     * @throws [CallRequestException.UnableToSendRequest] Submit is possible only with the status of "draft".
     * @throws [CallRequestException.NoInstitutionAdded] Unable to submit form to which no institute has been added.
     */
    fun submit() {
        if (status != Status.DRAFT) {
            throw CallRequestException.UnableToSendRequest(id)
        }

        if (content.institutions.isEmpty()) {
            throw CallRequestException.NoInstitutionAdded(id)
        }

        changeStatus(Status.SUBMITTED)
    }

    /**
     * Close call request.
     *
     * @throws [CallRequestException.AttemptToCloseUnapprovedRequest] You can only close a request confirmed by the
     *                                                              coordinators.
     */
    fun close() {
        if (status != Status.APPROVED) {
            throw CallRequestException.AttemptToCloseUnapprovedRequest(id)
        }

        changeStatus(Status.CLOSED)
    }

    /**
     * Confirm the correctness of data for the institute.
     *
     * @throws [CallRequestException.AttemptToApproveNotSubmittedRequest] You can approve only the request that was sent
     *                                                                  for consideration, or is already under
     *                                                                  consideration.
     * @throws [CallRequestException.CantFindAssociatedInstitute] Cant find associated institute for VaCoordinator.
     */
    fun approveByVACoordinator(id: UserAccountId) {
        if (status != Status.SUBMITTED && status != Status.BEING_HANDLED) {
            throw CallRequestException.AttemptToApproveNotSubmittedRequest(status)
        }

        /** If one institution picks up a request it should change to being handled */
        if (status == Status.SUBMITTED) {
            changeStatus(Status.BEING_HANDLED)
        }

        val institutionRequestForm = content.institutions
            .values
            .find { it.coordinatorId() == id }
            ?: throw CallRequestException.CantFindAssociatedInstitute(id)

        institutionRequestForm.approve()

        if (content.institutions.all { it.value.status() == Status.APPROVED }) {
            changeStatus(Status.APPROVED)
        }
    }

    /**
     * Cancel confirmation of the correctness of the entered data for the institute.
     *
     * @throws [CallRequestException.CantFindAssociatedInstitute] Cant find associated institute for VaCoordinator.
     */
    fun undoApproveByVACoordinator(id: UserAccountId) {
        val institutionRequestForm = content.institutions
            .values
            .find { it.coordinatorId() == id }
            ?: throw CallRequestException.CantFindAssociatedInstitute(id)

        institutionRequestForm.undoApprove()

        if (content.institutions.all { it.value.status() == Status.APPROVED }) {
            return changeStatus(Status.APPROVED)
        }

        changeStatus(Status.BEING_HANDLED)
    }

    /**
     * Delete call request (by its author or admin).
     *
     * @throws [CallRequestException.UnableToDeleteRequest] Call request can be deleted only in DRAFT status.
     */
    fun delete() {
        if (status != Status.DRAFT) {
            throw CallRequestException.UnableToDeleteRequest(id)
        }

        deletedAt = LocalDateTime.now()
    }

    /**
     * Withdraw the call request (performed by the administrator).
     */
    fun withdrawRequest() = changeStatus(Status.WITHDRAWN)

    /**
     * @throws [FormBuilderExceptions.FieldNotFound]
     * @throws [FormBuilderExceptions.FieldTypeMismatch]
     */
    private fun setValueToGeneralForm(fieldId: FieldId, value: FieldValue?) =
        content.general.setFieldValue(fieldId, value)

    /**
     * @throws [CallRequestException.InstitutionNotAdded]
     * @throws [FormBuilderExceptions.FieldNotFound]
     * @throws [FormBuilderExceptions.FieldTypeMismatch]
     */
    private fun setValueToInstitutionForm(institutionId: InstitutionId, fieldId: FieldId, value: FieldValue?) {
        val institutionRequestForm = content.institutions[institutionId]
            ?: throw CallRequestException.InstitutionNotAdded(institutionId)

        institutionRequestForm.content().setFieldValue(fieldId, value)
    }

    private fun changeStatus(new: Status) {
        status = new
    }

    /**
     * Extract specified field value from general form
     */
    private fun generalFormValue(field: FieldId): String? {
        val fieldValue = content.general
            .values[field]
            ?.value as? FieldValue.Text
            ?: return null

        return fieldValue.value
    }
}
