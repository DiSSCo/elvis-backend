package org.synthesis.keycloak.api

import java.util.*
import org.keycloak.representations.idm.authorization.ResourceRepresentation
import org.keycloak.representations.idm.authorization.ScopeRepresentation

data class KeycloakResourceId(
    val id: UUID
)

internal fun Resource.toKeycloakRepresentation() = ResourceRepresentation().apply {
    name = title()
    type = this@toKeycloakRepresentation.type
    scopes = this@toKeycloakRepresentation.scopes.map { it.toKeycloakRepresentation() }.toSet()
    attributes = this@toKeycloakRepresentation.attributes.map {
        it.key to listOf(it.value)
    }.toMap()
}

internal fun Scope.toKeycloakRepresentation() = ScopeRepresentation().apply {
    name = value
}

private fun Resource.title(): String {
    if ("$name [$id]".length < 250) {
        return "$name [$id]"
    }

    return id
}
