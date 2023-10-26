package edu.austral.ingsis.clientserver

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import edu.austral.ingsis.clientserver.serialization.Deserializer
import org.slf4j.Logger
import java.nio.charset.Charset

class MessageListenerContainer(private val logger: Logger, private val deserializer: Deserializer) {
    private val listeners by lazy { mutableMapOf<String, MessageAdapter<*>>() }

    fun <P : Any> setListenerFor(
        messageType: String,
        messageTypeReference: TypeReference<Message<P>>,
        listener: MessageListener<P>,
    ) {
        logger.info("Listener set for type: $messageType")

        val adapter = MessageAdapter(deserializer, messageTypeReference, listener)
        listeners[messageType] = adapter
    }

    fun unsetListenerFor(messageType: String) {
        logger.info("Listener unset for type: $messageType")

        listeners.remove(messageType)
    }

    fun handleMessage(stringMessage: String) {
        val byteArray = stringMessage.toByteArray(Charset.defaultCharset())
        val message = deserializer.deserialize(byteArray, jacksonTypeRef<Message<Any>>())

        listeners[message.type]?.handle(byteArray)
    }
}
