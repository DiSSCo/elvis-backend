package org.synthesis.formbuilder

import org.synthesis.institution.FacilityFieldValueResponse

fun DynamicForm.normalize(): Map<String, Any> {
    val result: MutableMap<String, Any> = mutableMapOf()

    all().forEach {
        when (it) {
            is StoredValue.Single -> result[it.fieldId.toString()] = it.value.asView()
            is StoredValue.Group -> {
                val fieldGroupValues: MutableMap<Int, Any> = mutableMapOf()

                it.values.forEach { (fieldId, fieldValue) ->

                    val position = fieldId.groupId?.position

                    if (position != null) {
                        if (!fieldGroupValues.containsKey(position)) {
                            fieldGroupValues[position] = mutableMapOf<String, Any>(
                                fieldId.id to fieldValue.asView()
                            )
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            val section = fieldGroupValues[position] as MutableMap<String, Any>
                            section[fieldId.id] = fieldValue.asView()
                        }
                    }
                }

                result[it.groupId.id] = fieldGroupValues
            }
        }
    }

    return result
}

private fun FieldWithValue.asView() = FacilityFieldValueResponse(
    type = type.toString().lowercase(),
    value = clearValue()
)
