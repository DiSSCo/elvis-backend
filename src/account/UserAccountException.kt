package org.synthesis.account

import org.synthesis.infrastructure.ApplicationException

sealed class UserAccountException(message: String?) : ApplicationException(message) {

    class NotFound : UserAccountException("Customer was not found")

    class AlreadyRegistered : UserAccountException("Customer already registered")
    class OperationFailed(error: Exception) : UserAccountException(error.message)

    class UpdateFailed(id: UserAccountId, error: Exception) :
        UserAccountException("Unable to update user with id `${id.uuid}`: ${error.message}")

    class RegistrationFailed(email: String, error: Exception) :
        UserAccountException("Unable to register with email `$email`:  ${error.message}")

    class IncorrectAttributes(id: UserAccountId, attribute: String) :
        UserAccountException("Incorrect configuration of the `$attribute` attribute for user `${id.uuid}`")
}
