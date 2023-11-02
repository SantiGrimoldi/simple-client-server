package edu.austral.ingsis.clientserver.netty.server

import com.fasterxml.jackson.core.type.TypeReference
import edu.austral.ingsis.clientserver.Message
import edu.austral.ingsis.clientserver.MessageAdapter
import edu.austral.ingsis.clientserver.MessageListener
import edu.austral.ingsis.clientserver.Server
import edu.austral.ingsis.clientserver.ServerBuilder
import edu.austral.ingsis.clientserver.ServerConnectionListener
import edu.austral.ingsis.clientserver.serialization.Deserializer
import edu.austral.ingsis.clientserver.serialization.Serializer
import edu.austral.ingsis.clientserver.serialization.json.JsonDeserializer
import edu.austral.ingsis.clientserver.serialization.json.JsonSerializer

class NettyServerBuilder(
    private val deserializer: Deserializer,
    private val serializer: Serializer,
) : ServerBuilder {
    companion object {
        fun createDefault() = NettyServerBuilder(JsonDeserializer(), JsonSerializer())
    }

    private var port: Int? = null
    private var connectionListener: ServerConnectionListener? = null
    private val messageListeners: MutableMap<String, MessageAdapter<*>> = mutableMapOf()

    override fun withPort(port: Int) = runInThis { this.port = port }

    override fun withConnectionListener(listener: ServerConnectionListener) = runInThis {
        this.connectionListener = listener
    }

    override fun <P : Any> addMessageListener(
        messageType: String,
        messageTypeReference: TypeReference<Message<P>>,
        messageListener: MessageListener<P>,
    ) = runInThis {
        messageListeners[messageType] = MessageAdapter(deserializer, messageTypeReference, messageListener)
    }

    private fun runInThis(block: NettyServerBuilder.() -> Unit): NettyServerBuilder = run {
        block(this)
        this
    }

    override fun build(): Server {
        requireNotNull(port) { "port is required" }

        return NettyServer(
            port!!,
            deserializer,
            serializer,
            connectionListener,
            messageListeners,
        )
    }
}
