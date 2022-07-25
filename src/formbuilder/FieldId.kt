package org.synthesis.formbuilder

data class FieldId(
    val id: String,
    val groupId: GroupId? = null,
    val position: Int? = null
) {
    override fun toString(): String {
        if (groupId != null) {
            return "$groupId.$id${position.positionToString()}"
        }

        return "$id${position.positionToString()}"
    }

    companion object {
        /**
         * Converts strings like this: catalog[42].item[12] into [FieldId]
         * where catalog[42] should be a GroupId(id=catalog,position=42)
         *
         * And full FieldId is FieldId(id=item,groupId=GroupId(...),position=12)
         *
         * @throws [FormBuilderExceptions.ParseFieldFailed]
         */
        fun fromString(str: String): FieldId {
            val tuple = str
                .trim('.')
                .split('.')
                .mapNotNull { format.find(it)?.groups }
                .map {
                    /** id and position */
                    listOf(it[1], it[3])
                }

            return when (tuple.size) {
                1 -> FieldId(
                    id = tuple[0].id(),
                    position = tuple[0].position()
                )
                2 -> FieldId(
                    id = tuple[1].id(),
                    position = tuple[1].position(),
                    groupId = GroupId(
                        id = tuple[0].id(),
                        position = tuple[0].position()
                    )
                )
                else -> throw FormBuilderExceptions.ParseFieldFailed("Cant parse field template")
            }
        }
    }
}

data class GroupId(
    val id: String,
    val position: Int? = null
) {
    override fun toString(): String = "$id${position.positionToString()}"

    companion object {

        /**
         * @throws [FormBuilderExceptions.ParseFieldFailed]
         */
        fun fromString(str: String): GroupId {
            val groups = format
                .find(str)
                ?.groups
                ?.toList()
                ?: throw FormBuilderExceptions.ParseFieldFailed("Cant parse field groups")

            return GroupId(
                id = groups.groupId(),
                position = groups.groupPosition()
            )
        }
    }
}

private val format = """^(\w+)(\[(\d+)])?$""".toRegex()
private fun Int?.positionToString() = if (this != null) "[$this]" else ""
private fun List<MatchGroup?>.id() = this[0]?.value
    ?: throw FormBuilderExceptions.ParseFieldFailed("Cant parse field identifier")

private fun List<MatchGroup?>.position() = this[1]?.value?.toInt()
private fun List<MatchGroup?>.groupId() = this[1]?.value
    ?: throw FormBuilderExceptions.ParseFieldFailed("Cant parse field identifier")

private fun List<MatchGroup?>.groupPosition() = this[3]?.value?.toInt()
