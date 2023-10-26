package edu.austral.ingsis.clientserver.serialization

import com.fasterxml.jackson.core.type.TypeReference

interface Deserializer {
    fun <V : Any> deserialize(byteArray: ByteArray, typeReference: TypeReference<V>): V
}
