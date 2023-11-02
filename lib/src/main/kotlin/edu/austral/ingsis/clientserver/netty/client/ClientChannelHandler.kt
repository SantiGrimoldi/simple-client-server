package edu.austral.ingsis.clientserver.netty.client

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import edu.austral.ingsis.clientserver.Message
import edu.austral.ingsis.clientserver.MessageAdapter
import edu.austral.ingsis.clientserver.serialization.Deserializer
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

internal class ClientChannelHandler(
    private val deserializer: Deserializer,
    private val serverConnectionContainer: ServerConnectionContainer,
    private val messageListeners: Map<String, MessageAdapter<*>>,
) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        serverConnectionContainer.handleConnection(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        serverConnectionContainer.handleClientConnectionClosed()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val byteArray = msg as ByteArray
        val rawMessage = deserializer.deserialize(byteArray, jacksonTypeRef<Message<Any>>())

        messageListeners[rawMessage.type]?.handle(byteArray)
    }
}
