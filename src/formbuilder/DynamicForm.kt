package org.synthesis.formbuilder

class DynamicForm(
    var values: MutableMap<FieldId, FieldWithValue>
) {
    companion object {
        fun create(values: Map<FieldId, Class<out FieldValue>>) =
            values
                .mapValues { (fieldId, valueType) -> FieldWithValue.withoutValue(fieldId, valueType) }
                .let { DynamicForm(it.toMutableMap()) }

        fun create(values: List<FieldWithValue>) = values
            .associateBy { it.field }
            .toMutableMap()
            .let { DynamicForm(it) }
    }

    init {
        /** Make sure all positions during init are 0 or null */
        assert(
            values.all {
                (it.key.groupId?.position ?: 0) == 0 && (it.key.position ?: 0) == 0
            }
        )
    }

    fun all(): List<StoredValue> = values.map { (fieldId, fieldWithValue) ->
        if (fieldId.groupId == null) {
            StoredValue.Single(
                fieldId = fieldId,
                value = fieldWithValue
            )
        } else {
            StoredValue.Group(
                groupId = fieldId.groupId,
                values = groupValues(fieldId.groupId)
            )
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun groupValues(id: GroupId): Map<FieldId, FieldWithValue> = values
        .filter {
            val groupId = it.key.groupId
            groupId != null && groupId.id == id.id
        }

    /**
     * @throws [FormBuilderExceptions.FieldNotFound]
     * @throws [FormBuilderExceptions.FieldTypeMismatch]
     */
    fun setFieldValue(fieldId: FieldId, fieldValue: FieldValue?) {
        val typeToCheck = values[fieldId.normaliseForLookup()]?.type
            ?: throw FormBuilderExceptions.FieldNotFound(fieldId)

        if (fieldValue != null && typeToCheck != fieldValue.extractType()) {
            throw FormBuilderExceptions.FieldTypeMismatch(fieldId)
        }

        values[fieldId] = FieldWithValue(
            field = fieldId,
            type = typeToCheck,
            value = fieldValue
        )
    }

    fun extractFieldValue(fieldId: FieldId): FieldWithValue? = values[fieldId]

    fun deleteGroup(id: GroupId) {
        val groupValues = groupValues(id)

        /**
         * Dirty hack: when deleting a group, if it is the only one, we need to nullify the values, and not delete
         * the whole group.
         */
        if (groupValues.size == 2 || id.position == 0) {
            groupValues.forEach { (fieldId, fieldWithValue) ->
                values[fieldId] = fieldWithValue.copy(
                    value = null
                )
            }
        } else {
            values = values.filterKeys { it.groupId != id }.toMutableMap()
        }
    }

    /**
     * In order to be able to insert fields with different positions
     * we need to be able to
     */
    private fun FieldId.normaliseForLookup() = FieldId(
        id = id,
        groupId = groupId?.normaliseForLookup(),
        position = position?.let { 0 }
    )

    private fun GroupId.normaliseForLookup() = GroupId(
        id = id,
        position = position?.let { 0 }
    )
}

sealed class StoredValue {
    data class Single(
        val fieldId: FieldId,
        val value: FieldWithValue
    ) : StoredValue()

    data class Group(
        val groupId: GroupId,
        val values: Map<FieldId, FieldWithValue>
    ) : StoredValue()
}
