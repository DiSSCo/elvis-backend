package org.synthesis.account.manage

import io.ktor.application.*
import org.synthesis.account.UserAccountId
import org.synthesis.infrastructure.IncorrectRequestParameters
import org.synthesis.infrastructure.ktor.receiveUuidFromParameters

/**
 * Extract user id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.userAccountId(): UserAccountId = try {
    UserAccountId(receiveUuidFromParameters("userAccountId"))
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "userAccountId",
        message = "Failed to retrieve user id"
    )
}
