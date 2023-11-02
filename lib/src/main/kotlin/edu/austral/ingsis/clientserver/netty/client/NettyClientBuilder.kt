package edu.austral.ingsis.clientserver.netty.client

import com.fasterxml.jackson.core.type.TypeReference
import edu.austral.ingsis.clientserver.Client
import edu.austral.ingsis.clientserver.ClientBuilder
import edu.austral.ingsis.clientserver.ClientConnectionListener
import edu.austral.ingsis.clientserver.Message
import edu.austral.ingsis.clientserver.MessageAdapter
import edu.austral.ingsis.clientserver.MessageListener
import edu.austral.ingsis.clientserver.serialization.Deserializer
import edu.austral.ingsis.clientserver.serialization.Serializer
import edu.austral.ingsis.clientserver.serialization.json.JsonDeserializer
import edu.austral.ingsis.clientserver.serialization.json.JsonSerializer
import java.net.SocketAddress

class NettyClientBuilder(
    private val deserializer: Deserializer,
    private val serializer: Serializer,
) : ClientBuilder {
    companion object {
        fun createDefault() = NettyClientBuilder(JsonDeserializer(), JsonSerializer())
    }

    private var address: SocketAddress? = null
    private var connectionListener: ClientConnectionListener? = null
    private val messageListeners: MutableMap<String, MessageAdapter<*>> = mutableMapOf()

    override fun withAddress(address: SocketAddress) = runInThis {
        this.address = address
    }

    override fun withConnectionListener(listener: ClientConnectionListener) = runInThis {
        this.connectionListener = listener
    }

    override fun <P : Any> addMessageListener(
        messageType: String,
        messageTypeReference: TypeReference<Message<P>>,
        messageListener: MessageListener<P>,
    ) = runInThis {
        messageListeners[messageType] = MessageAdapter(deserializer, messageTypeReference, messageListener)
    }

    private fun runInThis(block: NettyClientBuilder.() -> Unit): NettyClientBuilder = run {
        block(this)
        this
    }

    override fun build(): Client {
        requireNotNull(address) { "address cannot be null" }

        return NettyClient(
            address!!,
            deserializer,
            serializer,
            connectionListener,
            messageListeners,
        )
    }
}
