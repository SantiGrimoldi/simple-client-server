package edu.austral.ingsis.clientserver.serialization.json

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import edu.austral.ingsis.clientserver.serialization.Deserializer

class JsonDeserializer : Deserializer {
    private val mapper: ObjectMapper = ObjectMapper()

    init {
        mapper.registerModule(KotlinModule.Builder().build())
    }

    override fun <V : Any> deserialize(byteArray: ByteArray, typeReference: TypeReference<V>): V {
        return mapper.readValue(byteArray, typeReference)
    }
}
