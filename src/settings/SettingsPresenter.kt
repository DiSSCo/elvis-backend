package org.synthesis.settings

import org.synthesis.infrastructure.IncorrectRequestParameters

class SettingsPresenter(
    private val settingsStore: SettingsStore
) {
    suspend fun obtain(key: String): SettingsParameter {
        val parameter = settingsStore.find(key)
            ?: throw IncorrectRequestParameters(mapOf("optionKey" to "Parameter with key `$key` was not found"))

        return parameter.asStructure()
    }
}
