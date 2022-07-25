package org.synthesis.keycloak.admin

class KeycloakUserGroup(
    private val value: String
) {
    /**
     * In Keycloak, groups are stored as strings with leading slashes.
     */
    fun toApplicationRepresentation(): String = value.replace("/", "")

    override fun toString(): String = value
}
