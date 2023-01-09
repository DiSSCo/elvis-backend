package org.synthesis.institution

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.flow.toList
import org.koin.ktor.ext.inject
import org.synthesis.account.UserAccountId
import org.synthesis.attachment.*
import org.synthesis.auth.interceptor.authenticatedUser
import org.synthesis.auth.ktor.withRole
import org.synthesis.infrastructure.*
import org.synthesis.infrastructure.ktor.*
import org.synthesis.institution.facility.*
import org.synthesis.institution.members.InstitutionMembersProvider
import org.synthesis.institution.view.InstitutionPresenter

/**
 * Routes:
 * GET  /institutions/institution_view List of all institutes.
 * GET  /institutions/{institutionId}  Receive information about the institute.
 * POST /institutions/                 Add new institution.
 *
 * POST /institutions/{institutionId}/setFormValue Update institution details form.
 * POST /institutions/{institutionId}/delete-group Delete institution details form group.
 * POST /institutions/{institutionId}/change_name  Change institution name.
 *
 * POST /institutions/{institutionId}/facilities                            Create a new facility.
 * GET  /institutions/{institutionId}/facilities/{facilityId}               View facility.
 * POST /institutions/{institutionId}/facilities/{facilityId}/setFormValue  Set facility form value.
 * POST /institutions/{institutionId}/facilities/{facilityId}/delete Remove existing facility.
 * POST /institutions/{institutionId}/facilities/{facilityId}/delete-group  Remove facility field group
 *
 * POST /institutions/{institutionId}/facilities/{facilityId}/images                  Upload facility image.
 * GET  /institutions/{institutionId}/facilities/{facilityId}/images/{imageId}        Obtain facility image
 * POST /institutions/{institutionId}/facilities/{facilityId}/images/{imageId}/remove Remove facility image.
 *
 * GET  /institutions/{institutionId}/people List of the institution members.
 * POST /institutions/{institutionId}/people/invite Invite user to the institute.
 * POST /institutions/{institutionId}/people/remove Remove user from the institute.
 */
@Suppress("LongMethod")
fun Route.institutionRoutes() {
    val presenter by inject<InstitutionPresenter>()
    val institutionProvider by inject<InstitutionProvider>()
    val attachmentProvider by inject<AttachmentProvider>()
    val facilityProvider by inject<FacilityProvider>()
    val facilityPresenter by inject<FacilityPresenter>()
    val institutionMembersProvider by inject<InstitutionMembersProvider>()
    val institutionAlreadyAddedMessage = "institute with the specified GRID already exists"

    route("/institutions/") {
        /**
         * List of all institutes.
         */
        get("/institution_view") {
            call.respondSuccess(
                data = presenter.findAll()
            )
        }

        /**
         * Receive information about the institute.
         */
        get("/{institutionId}") {
            val institutionId = call.institutionId()

            call.respondSuccess(
                data = presenter.find(institutionId)
                    ?: throw IncorrectRequestParameters.create(
                        "institutionId",
                        "Institution not found"
                    )
            )
        }

        authenticate {
            withRole("institution_add") {}
            /**
             * Add new institution.
             */
            post {

                val command = call.receiveValidated<InstitutionCommand.Add>()

                try {
                    institutionProvider.handle(command)

                    call.respondCreated("Institution added")
                } catch (e: InstituteException.InstitutionAlreadyAdded) {
                    throw IncorrectRequestParameters.create(
                        field = "id",
                        message = institutionAlreadyAddedMessage
                    )
                }
            }
        }

        authenticate {
            withRole("institution_edit") {}
            /**
             * Update institution details form.
             */
            post("/{institutionId}/setFormValue") {
                val command = call.receiveValidated<InstitutionCommand.Update>()
                try {
                    institutionProvider.handle(command, call.institutionId())
                    call.respondCreated("Institution updated")
                } catch (e: InstituteException.InstitutionAlreadyAdded) {
                    throw IncorrectRequestParameters.create(
                        field = "id",
                        message = institutionAlreadyAddedMessage
                    )
                }
            }

            /**
             * Delete institution details form group.
             */

            post("/{institutionId}/delete-group") {
                val command = call.receiveValidated<InstitutionCommand.RemoveGroup>()
                institutionProvider.handle(command, call.institutionId())
                call.respondSuccess()
            }


            /**
             * Change institution name.
             */
            post("/{institutionId}/change_name") {
                val command = call.receiveValidated<InstitutionCommand.ChangeName>()
                try {
                    institutionProvider.handle(command, call.institutionId())
                    call.respondCreated("Institution updated")
                } catch (e: InstituteException.InstitutionAlreadyAdded) {
                    throw IncorrectRequestParameters.create(
                        field = "id",
                        message = institutionAlreadyAddedMessage
                    )
                }
            }
        }

        route("/{institutionId}/facilities") {

            /**
             * Create a new facility
             */
            authenticate {
                withRole("facility_create") {}
                post {
                    val facilityId = FacilityId.next()

                    facilityProvider.handle(
                        FacilityCommand.Create(
                            id = facilityId,
                            institutionId = call.institutionId(),
                            moderatorId = authenticatedUser().id
                        )
                    )

                    call.respondCreated(
                        "Facility successful created",
                        mapOf("id" to facilityId.uuid)
                    )
                }
            }


            authenticate {
                withRole("facility_edit") {}
                /**
                 * Set facility form value.
                 */

                post("/{facilityId}/setFormValue") {
                    val formData = call.receiveValidated<SetFacilityDataRequest>()

                    facilityProvider.handle(
                        FacilityCommand.SetField(
                            id = call.facilityId(),
                            fieldId = formData.fieldId,
                            fieldValue = formData.fieldValue
                        )
                    )

                    call.respondSuccess()
                }

                /**
                 * Upload facility image.
                 */
                post("/{facilityId}/images") {

                    val facilityId = call.facilityId()
                    val storedFiles: MutableList<AttachmentId> = mutableListOf()

                    call.receiveFiles().forEach {
                        storedFiles.add(
                            attachmentProvider.store(
                                originalName = it.originalFileName,
                                payload = it.content.toByteArray(),
                                metadata = AttachmentMetadata(
                                    extension = it.extension,
                                    mimeType = AttachmentMimeType(
                                        base = it.contentType.contentType,
                                        subType = it.contentType.contentSubtype
                                    )
                                ),
                                to = AttachmentCollection(facilityId.uuid.toString())
                            )
                        )
                    }

                    facilityProvider.handle(
                        FacilityCommand.AddImages(
                            id = facilityId,
                            storedFiles = storedFiles
                        )
                    )

                    call.respondSuccess(data = storedFiles)
                }
            }


            /**
             * Obtain facility image.
             */
            get("/{facilityId}/images/{imageId}") {
                val facilityId = call.facilityId()
                val imageId = call.imageId()

                val image = facilityPresenter.obtainImage(facilityId, imageId)
                    ?: throw IncorrectRequestParameters.create(
                        field = "imageId",
                        message = "Unable to find image `$imageId` for facility `$facilityId`"
                    )

                call.respondSuccess(data = image)
            }

            authenticate {
                withRole("facility_edit") {}

                /**
                 * Remove facility image.
                 */
                post("/{facilityId}/images/{imageId}/remove") {
                    val facilityId = call.facilityId()
                    val imageId = call.imageId()
                    attachmentProvider.remove(
                        id = AttachmentId(imageId),
                        from = AttachmentCollection(facilityId.uuid.toString())
                    )
                    facilityProvider.handle(
                        FacilityCommand.RemoveImages(
                            id = facilityId,
                            storedImage = AttachmentId(imageId)
                        )
                    )
                    call.respondSuccess()
                }

                /**
                 * Remove facility field group
                 */
                post("/{facilityId}/delete-group") {
                    val request = call.receiveValidated<RemoveFieldGroupRequest>()

                    facilityProvider.handle(
                        FacilityCommand.RemoveGroup(
                            id = call.facilityId(),
                            groupId = request.groupId
                        )
                    )

                    call.respondSuccess()
                }
            }


            authenticate {
                withRole("facility_delete") {}

                /**
                 * Remove existing facility
                 */
                post("/{facilityId}/delete") {
                    facilityProvider.handle(
                        FacilityCommand.Remove(call.facilityId())
                    )

                    call.respondSuccess()
                }
            }


            authenticate {
                withRole("facility_view") {}
                /**
                 * Receive facility information
                 */
                get("/{facilityId}") {
                    val facility = facilityPresenter.find(call.facilityId())
                        ?: throw IncorrectRequestParameters.create(
                            "facilityId",
                            "Facility not found"
                        )

                    call.respondSuccess(facility)
                }
            }
        }

        route("/{institutionId}/people") {
            authenticate {

                /**
                 * List of the institution members.
                 */
                get {
                    call.respondCollection(
                        institutionMembersProvider.list(call.institutionId()).toList()
                    )
                }


                /**
                 * Invite user to the institute.
                 */
                post("/invite") {
                    institutionMembersProvider.attach(
                        id = UserAccountId(call.receiveValidated<ManageInstituteMember>().id),
                        toInstitutionId = call.institutionId()
                    )

                    call.respondSuccess()
                }

                //TODO doesn't this need to be behind a role?
                /**
                 * Remove user from the institute.
                 */
                post("/remove") {
                    institutionMembersProvider.detach(
                        id = UserAccountId(call.receiveValidated<ManageInstituteMember>().id)
                    )

                    call.respondSuccess()
                }
            }
        }
    }
}
