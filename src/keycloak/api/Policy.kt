package org.synthesis.keycloak.api

/**
 * @url {keycloakHost}/auth/admin/master/console/#/realms/{realmId}/clients/{clientId}/authz/resource-server/scope
 */
data class Scope(
    val value: String
)

/**
 * @url {keycloakHost}/auth/admin/master/console/#/realms/{realmId}/clients/{clientId}/authz/resource-server/resource
 */
data class Resource(
    val id: String,
    val name: String,
    val type: String,
    val scopes: List<Scope>,
    val attributes: Map<String, String>
)

/**
 * @url {keycloakHost}/auth/admin/master/console/#/realms/{realmId}/clients/{clientId}/authz/resource-server/policy
 */
data class Permission(
    val resource: String,
    val scope: Scope
) {
    override fun toString(): String = "$resource#${scope.value}"
}
