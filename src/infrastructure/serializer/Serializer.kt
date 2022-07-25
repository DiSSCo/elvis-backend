package org.synthesis.infrastructure.serializer

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.synthesis.formbuilder.FieldId

interface Serializer {
    fun <T> serialize(structure: T): String
    fun <T> unserialize(payload: String, to: Class<T>): T
}

object JacksonSerializer : Serializer {
    private val objectMapper = jacksonObjectMapper()
    private val module = SimpleModule()

    init {
        module.addKeyDeserializer(FieldId::class.java, FieldIdSerializer)
        objectMapper.registerModule(module)
    }

    object FieldIdSerializer : KeyDeserializer() {
        override fun deserializeKey(key: String?, ctxt: DeserializationContext?): Any? =
            if (key != null) FieldId.fromString(key) else null
    }

    override fun <T> serialize(structure: T): String = objectMapper.writeValueAsString(structure)

    override fun <T> unserialize(payload: String, to: Class<T>): T = objectMapper.readValue(payload, to)
}
