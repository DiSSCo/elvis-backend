package org.synthesis.account.registration

import org.slf4j.Logger
import org.synthesis.account.*
import org.synthesis.account.manage.CreateUserAccountRequest
import org.synthesis.account.manage.UserAccountProvider
import org.synthesis.infrastructure.mailer.MailEnvelope
import org.synthesis.infrastructure.mailer.Mailer
import org.synthesis.infrastructure.mailer.SendmailResult

sealed class UserAccountRegistrationCredentials {
    data class ClearPassword(
        val password: String
    ) : UserAccountRegistrationCredentials()

    data class HashedPassword(
        val hash: String,
        val algorithm: String = "bcrypt",
        val iterations: Int = 10
    ) : UserAccountRegistrationCredentials()
}

data class UserAccountRegistrationData(
    val email: String,
    val groups: List<String>,
    val fullName: UserFullName,
    val attributes: UserAccountAttributes,
    val credentials: UserAccountRegistrationCredentials
)

interface RegistrationHandler {

    /**
     * New User Registration.
     *
     * If the [withNotification] flag is specified, then a welcome message will be sent to the email specified
     * during registration
     *
     * @throws [UserAccountException.AlreadyRegistered]
     * @throws [UserAccountException.RegistrationFailed]
     * @throws [IllegalStateException]
     */
    suspend fun handle(
        data: UserAccountRegistrationData,
        withNotification: Boolean
    ): UserAccountId
}

class KeycloakRegistrationHandler(
    private val userAccountProvider: UserAccountProvider,
    private val mailer: Mailer,
    private val logger: Logger

) : RegistrationHandler {

    /**
     * The user is created first in the local database, then (in the context of the transaction)
     * he is created in Keycloak.
     *
     * @throws [UserAccountException.AlreadyRegistered]
     * @throws [UserAccountException.RegistrationFailed]
     * @throws [IllegalStateException]
     */
    override suspend fun handle(data: UserAccountRegistrationData, withNotification: Boolean): UserAccountId {
        validate(data)

        return userAccountProvider.create(
            CreateUserAccountRequest(
                email = data.email,
                groups = data.groups,
                fullName = data.fullName,
                attributes = data.attributes,
                credentials = data.credentials
            )
        ).also {
                if (withNotification) {
                    val result = mailer.send(
                        MailEnvelope(
                            recipientEmail = data.email,
                            recipientFullName = data.fullName,
                            subject = "Your account for ELViS is created",
                            body = """
Your account for access to the European Loans and Visits System (ELViS) was successfully created.<br>
Please follow <a href="{frontendUrl}">this link</a> to access your ELViS account, using this email address and the 
password you have registered
                """.trimIndent()
                        )
                    )

                    if (result is SendmailResult.Failed) {
                        logger.error(
                            "Sending welcome message for `${result.recipientEmail}` failed: ${result.cause.message}"
                        )
                    }
                }
        }
    }

    private fun validate(data: UserAccountRegistrationData) {
        if ("requester" in data.groups && data.attributes.orcId == null) {
            throw UserAccountException.RegistrationFailed(
                email = data.email,
                error = Exception("The user who is in the group of `requesters` must have `orcId` attribute specified")
            )
        }

        if ("institution moderator" in data.groups && data.attributes.institutionId == null) {
            throw UserAccountException.RegistrationFailed(
                email = data.email,
                error = Exception(
                    "The user who is in the group of `moderators` must have `institutionId` attribute specified"
                )
            )
        }

        if ("va coordinator" in data.groups && data.attributes.institutionId == null) {
            throw UserAccountException.RegistrationFailed(
                email = data.email,
                error = Exception(
                    "The user who is in the group of `coordinators` must have `institutionId` attribute specified"
                )
            )
        }
    }
}
