package edu.austral.ingsis.clientserver

import com.fasterxml.jackson.core.type.TypeReference
import java.net.SocketAddress

interface ClientBuilder {
    fun withAddress(address: SocketAddress): ClientBuilder

    fun withConnectionListener(listener: ClientConnectionListener): ClientBuilder

    fun <P : Any> addMessageListener(
        messageType: String,
        messageTypeReference: TypeReference<Message<P>>,
        messageListener: MessageListener<P>,
    ): ClientBuilder

    fun build(): Client
}
