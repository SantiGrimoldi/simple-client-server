package edu.austral.ingsis.clientserver.serialization

interface Serializer {
    fun <V : Any> serialize(message: V): ByteArray
}
