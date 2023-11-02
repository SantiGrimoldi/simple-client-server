package edu.austral.ingsis.clientserver.netty.server

import edu.austral.ingsis.clientserver.ServerConnectionListener
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable

@Sharable
internal class ClientConnectionsContainer(private val connectionListener: ServerConnectionListener?) {
    private val clients = mutableMapOf<String, Channel>()

    fun handleClientConnection(channel: Channel) {
        val clientId = channel.id().asLongText()
        clients[clientId] = channel
        connectionListener?.handleClientConnection(clientId)
    }

    fun handleClientConnectionClosed(channel: Channel) {
        val clientId = channel.id().asLongText()

        clients.remove(clientId)
        connectionListener?.handleClientConnectionClosed(clientId)
    }

    fun send(clientId: String, content: ByteArray) {
        val clientChannel = clients[clientId]

        requireNotNull(clientChannel) { "Client $clientId is not registered" }

        sendToChannel(clientChannel, content)
    }

    fun broadcast(content: ByteArray) {
        clients.values.forEach { clientChannel ->
            sendToChannel(clientChannel, content)
        }
    }

    private fun sendToChannel(clientChannel: Channel, content: ByteArray) {
        val clientId = clientChannel.id().asLongText()

        require(clientChannel.isActive) { "Client $clientId is not active" }
        require(clientChannel.isRegistered) { "Client $clientId is not registered" }

        clientChannel.writeAndFlush(content).sync()
    }
}
