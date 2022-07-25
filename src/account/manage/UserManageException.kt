package org.synthesis.account.manage

sealed class UserManageException(message: String) : Exception(message) {
    class OperationFailed(type: String, message: String) :
        UserManageException("Operation `$type` failed with message: $message")
}
