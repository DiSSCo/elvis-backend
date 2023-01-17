package org.synthesis.country

import io.ktor.server.application.*
import org.synthesis.infrastructure.IncorrectRequestParameters

/**
 * Extract Country id from URI
 *
 * @throws [IncorrectRequestParameters]
 */
fun ApplicationCall.countryCode(): CountryCode = try {
    CountryCode(parameters["countryIsoCode"]?.uppercase() ?: throw Exception())
} catch (e: Exception) {
    throw IncorrectRequestParameters.create(
        field = "countryIsoCode",
        message = "Failed to retrieve country iso code"
    )
}
