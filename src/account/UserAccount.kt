package org.synthesis.account

import com.fasterxml.jackson.annotation.JsonValue
import io.ktor.auth.*
import java.time.LocalDateTime
import java.util.*

private val institutionRelatedGroups = listOf("institution moderator", "ta coordinator", "va coordinator")
private val countryRelatedGroups = listOf("ta scorer", "taf admin")

data class UserAccount(
    val id: UserAccountId,
    val realmId: String,
    val email: String,
    val groups: List<String>,
    val roles: List<String>,
    val fullName: UserFullName,
    val attributes: UserAccountAttributes,
    val status: UserAccountStatus,
    val synchronizedAt: LocalDateTime?
) : Principal

fun UserAccount.hasInstitutionRelatedGroup(): Boolean = linkedToContext(institutionRelatedGroups)
fun UserAccount.hasCountryRelatedRoles(): Boolean = linkedToContext(countryRelatedGroups)

private fun UserAccount.linkedToContext(contextGroups: List<String>): Boolean {
    for (group in contextGroups) {
        if (group in this.groups) {
            return true
        }
    }

    return false
}

data class UserFullName(
    val firstName: String,
    val lastName: String
) {
    override fun toString(): String = "$firstName $lastName"
}

data class UserAccountId(
    @JsonValue
    val uuid: UUID
) {
    override fun toString(): String = uuid.toString()
}
