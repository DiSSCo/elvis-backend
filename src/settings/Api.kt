package org.synthesis.settings

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import org.synthesis.infrastructure.IncorrectRequestParameters

data class SettingsParameter(
    @NotEmpty
    @NotBlank
    val key: String,
    val value: Any?,
    @NotEmpty
    @NotBlank
    val type: String
)

@Suppress("UnsafeCast")
internal fun SettingsParameter.asParameter() = Parameter(
    key = key,
    value = when (type) {
        "string" -> ParameterValue.StringValue(value as String?)
        "integer" -> ParameterValue.IntValue(value as Int?)
        else -> throw IncorrectRequestParameters.create("type", "Unsupported option value datatype")
    }
)

internal fun Parameter.asStructure() = SettingsParameter(
    key = key,
    type = when (value) {
        is ParameterValue.StringValue -> "string"
        is ParameterValue.IntValue -> "integer"
    },
    value = when (value) {
        is ParameterValue.StringValue -> value.value
        is ParameterValue.IntValue -> value.value
    }
)
