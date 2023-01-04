package org.synthesis.institution

import io.ktor.server.application.ApplicationCall
import java.util.*
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveFromParameters
import org.synthesis.infrastructure.ktor.receiveUuidFromParameters
import org.synthesis.institution.coordinator.CoordinatorType
import org.synthesis.institution.facility.FacilityId

/**
 * Extract institution id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.institutionId(): InstitutionId = try {
    InstitutionId.fromString(parameters["institutionId"] ?: throw Exception())
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "institutionId",
        message = "Failed to retrieve institution id"
    )
}

/**
 * Extract facility id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.facilityId(): FacilityId = try {
    FacilityId(receiveUuidFromParameters("facilityId"))
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "facilityId",
        message = "Failed to retrieve facility id"
    )
}

/**
 * Extract facility id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.imageId(): UUID = try {
    receiveUuidFromParameters("imageId")
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "imageId",
        message = "Failed to retrieve image id"
    )
}

/**
 * Extract coordinator type from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.coordinatorType(): CoordinatorType = try {
    CoordinatorType.valueOf(receiveFromParameters("coordinatorType").toUpperCase())
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "imageId",
        message = "Failed to retrieve coordinator type"
    )
}
