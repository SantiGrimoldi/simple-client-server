package edu.austral.ingsis.clientserver.netty.client

import edu.austral.ingsis.clientserver.ClientConnectionListener
import io.netty.channel.ChannelHandlerContext

internal class ServerConnectionContainer(private val connectionListener: ClientConnectionListener?) {
    private var serverChannel: ChannelHandlerContext? = null

    fun handleConnection(ctx: ChannelHandlerContext) {
        serverChannel = ctx
        connectionListener?.handleConnection()
    }

    fun handleClientConnectionClosed() {
        serverChannel = null
        connectionListener?.handleConnectionClosed()
    }

    fun send(content: ByteArray) {
        requireNotNull(serverChannel) { "Server is not connected" }
        serverChannel!!.writeAndFlush(content).sync()
    }
}
