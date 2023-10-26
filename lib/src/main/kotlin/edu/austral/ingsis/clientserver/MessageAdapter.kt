package edu.austral.ingsis.clientserver

import com.fasterxml.jackson.core.type.TypeReference
import edu.austral.ingsis.clientserver.serialization.Deserializer

class MessageAdapter<P : Any>(
    private val deserializer: Deserializer,
    private val messageTypeReference: TypeReference<Message<P>>,
    private val messageListener: MessageListener<P>,
) {
    fun handle(rawMessage: ByteArray) {
        val message = deserializer.deserialize(rawMessage, messageTypeReference)
        messageListener.handleMessage(message)
    }
}
