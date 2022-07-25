package org.synthesis.infrastructure.ktor

import io.ktor.application.ApplicationCall
import io.ktor.request.receive
import java.lang.IllegalArgumentException
import java.util.*
import javax.validation.Validation
import kotlin.reflect.KClass
import org.synthesis.infrastructure.IncorrectRequestParameters

private val validator = Validation.buildDefaultValidatorFactory().validator

suspend inline fun <reified T : Any> ApplicationCall.receiveValidated(): T = try {
    receiveValidated(T::class)
} catch (e: Exception) {
    throw IncorrectRequestParameters.create("body", "Unable to parse request JSON: ${e.message}")
}

suspend fun <T : Any> ApplicationCall.receiveValidated(type: KClass<T>): T {
    val structure = receive(type)
    val violations = validator.validate(structure)

    if (violations.count() > 0) {
        throw IncorrectRequestParameters(
            violations.map {
                it.propertyPath.toString() to it.message.toString()
            }.toMap()
        )
    }

    return structure
}

fun ApplicationCall.receiveFromParameters(key: String): String =
    parameters[key] ?: throw IncorrectRequestParameters(mapOf(key to "Parameter must be specified"))

fun ApplicationCall.receiveUuidFromParameters(key: String): UUID = try {
    UUID.fromString(receiveFromParameters(key))
} catch (e: IllegalArgumentException) {
    throw IncorrectRequestParameters(mapOf(key to "Parameter must contain correct UUID"))
}
