package edu.austral.ingsis.clientserver.serialization.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import edu.austral.ingsis.clientserver.serialization.Serializer

class JsonSerializer : Serializer {
    private val mapper: ObjectMapper = ObjectMapper()

    init {
        mapper.registerModule(KotlinModule.Builder().build())
    }

    override fun <V : Any> serialize(message: V): ByteArray {
        return mapper.writeValueAsBytes(message)
    }
}
