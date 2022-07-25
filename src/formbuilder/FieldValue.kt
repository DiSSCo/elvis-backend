package org.synthesis.formbuilder

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)

@JsonSubTypes(
    JsonSubTypes.Type(value = FieldValue.Text::class, name = "string"),
    JsonSubTypes.Type(value = FieldValue.Checkbox::class, name = "boolean"),
    JsonSubTypes.Type(value = FieldValue.List::class, name = "list")
)

sealed class FieldValue {
    data class Text(val value: String) : FieldValue()
    data class Checkbox(val value: Boolean) : FieldValue()
    data class List(val value: kotlin.collections.List<String> = listOf()) : FieldValue()
}

data class FieldWithValue(
    val field: FieldId,
    val type: Type,
    val value: FieldValue?
) {
    enum class Type {
        Boolean,
        String,
        List
    }

    companion object Factory {
        /**
         * Creates an empty value. Used when initializing a clean form
         *
         * @throws [FormBuilderExceptions.UnsupportedType]
         */
        fun withoutValue(fieldId: FieldId, valueType: Class<out FieldValue>) = FieldWithValue(
            field = fieldId,
            type = valueType.extractType(),
            value = null
        )
    }
}

fun FieldWithValue.clearValue(): Any? = when (this.value) {
    is FieldValue.Text -> this.value.value
    is FieldValue.Checkbox -> this.value.value
    is FieldValue.List -> this.value.value
    else -> null
}

/**
 * Create simple text field
 *
 * @throws [FormBuilderExceptions.ParseFieldFailed] Incorrect field identifier
 */
fun text(id: String) = FieldWithValue(
    field = FieldId.fromString(id),
    type = FieldWithValue.Type.String,
    value = null
)

/**
 * Create simple text field
 *
 * @throws [FormBuilderExceptions.ParseFieldFailed] Incorrect field identifier
 */
fun list(id: String, value: List<String>) = FieldWithValue(
    field = FieldId.fromString(id),
    type = FieldWithValue.Type.List,
    value = FieldValue.List(value)
)

/**
 * Create simple text field
 *
 * @throws [FormBuilderExceptions.ParseFieldFailed] Incorrect field identifier
 */
fun list(id: String) = FieldWithValue(
    field = FieldId.fromString(id),
    type = FieldWithValue.Type.List,
    value = FieldValue.List()
)

/**
 * Create simple text field with value
 *
 * @throws [FormBuilderExceptions.ParseFieldFailed] Incorrect field identifier
 */
fun text(id: String, value: String) = FieldWithValue(
    field = FieldId.fromString(id),
    type = FieldWithValue.Type.String,
    value = FieldValue.Text(value)
)

/**
 * Create checkbox field
 *
 * @throws [FormBuilderExceptions.ParseFieldFailed] Incorrect field identifier
 */
fun checkBox(id: String) = FieldWithValue(
    field = FieldId.fromString(id),
    type = FieldWithValue.Type.Boolean,
    value = null
)

/**
 * Create checkbox field with value
 *
 * @throws [FormBuilderExceptions.ParseFieldFailed] Incorrect field identifier
 */
fun checkBox(id: String, checked: Boolean) = FieldWithValue(
    field = FieldId.fromString(id),
    type = FieldWithValue.Type.Boolean,
    value = FieldValue.Checkbox(checked)
)

/**
 * Retrieves the field type of a data type
 *
 * @throws [FormBuilderExceptions.UnsupportedType]
 */
internal fun Class<out FieldValue>.extractType() = when (this) {
    FieldValue.Text::class.java -> FieldWithValue.Type.String
    FieldValue.Checkbox::class.java -> FieldWithValue.Type.Boolean
    FieldValue.List::class.java -> FieldWithValue.Type.List
    else -> throw FormBuilderExceptions.UnsupportedType(this)
}

internal fun FieldValue.extractType() = when (this) {
    is FieldValue.Text -> FieldWithValue.Type.String
    is FieldValue.Checkbox -> FieldWithValue.Type.Boolean
    is FieldValue.List -> FieldWithValue.Type.List
}
