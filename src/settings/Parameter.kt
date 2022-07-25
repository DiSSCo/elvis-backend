package org.synthesis.settings

data class Parameter(
    val key: String,
    val value: ParameterValue
)

sealed class ParameterValue {
    data class StringValue(
        val value: String?
    ) : ParameterValue()

    data class IntValue(
        val value: Int?
    ) : ParameterValue()
}
