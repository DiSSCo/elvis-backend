package org.synthesis.account

import java.time.LocalDateTime

sealed class UserAccountStatus {
    data class Banned(
        val active: Boolean = false,
        val dateTime: LocalDateTime,
        val reason: String
    ) : UserAccountStatus()

    data class Inactive(
        val active: Boolean = false
    ) : UserAccountStatus()

    data class Active(
        val active: Boolean = true
    ) : UserAccountStatus()
}
