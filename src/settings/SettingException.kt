package org.synthesis.settings

sealed class SettingException(message: String?) : Exception(message) {
    class UnsupportedType(type: String) : SettingException("Unsupported option type `$type` specified")
}
