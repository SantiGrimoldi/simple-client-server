package edu.austral.ingsis.clientserver

import com.fasterxml.jackson.core.type.TypeReference

interface ServerBuilder {
    fun withPort(port: Int): ServerBuilder

    fun withConnectionListener(listener: ServerConnectionListener): ServerBuilder

    fun <P : Any> addMessageListener(
        messageType: String,
        messageTypeReference: TypeReference<Message<P>>,
        messageListener: MessageListener<P>,
    ): ServerBuilder

    fun build(): Server
}
