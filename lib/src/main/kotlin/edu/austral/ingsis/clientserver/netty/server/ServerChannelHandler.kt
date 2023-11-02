package edu.austral.ingsis.clientserver.netty.server

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import edu.austral.ingsis.clientserver.Message
import edu.austral.ingsis.clientserver.MessageAdapter
import edu.austral.ingsis.clientserver.serialization.Deserializer
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

@Sharable
internal class ServerChannelHandler(
    private val deserializer: Deserializer,
    private val clientConnectionsContainer: ClientConnectionsContainer,
    private val messageListeners: Map<String, MessageAdapter<*>>,
) : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        clientConnectionsContainer.handleClientConnection(ctx.channel())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        clientConnectionsContainer.handleClientConnectionClosed(ctx.channel())
    }

    override fun channelRead(ctx: ChannelHandlerContext, data: Any) {
        val byteArray = data as ByteArray
        val rawMessage = deserializer.deserialize(byteArray, jacksonTypeRef<Message<Any>>())

        messageListeners[rawMessage.type]?.handle(byteArray)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
