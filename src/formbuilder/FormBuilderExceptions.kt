package org.synthesis.formbuilder

import org.synthesis.infrastructure.ApplicationException

sealed class FormBuilderExceptions(message: String? = null) : ApplicationException(message) {
    class ParseFieldFailed(message: String?) : FormBuilderExceptions(message)
    class FieldNotFound(id: FieldId) : FormBuilderExceptions("Field `${id.id}` not found")
    class FieldTypeMismatch(id: FieldId) : FormBuilderExceptions("Field `${id.id}` type mismatch")
    class UnsupportedType(type: Class<out FieldValue>) :
        FormBuilderExceptions("Unsupported value type: `$type`")
}
