package edu.austral.ingsis.clientserver.netty.client

import edu.austral.ingsis.clientserver.Client
import edu.austral.ingsis.clientserver.ClientConnectionListener
import edu.austral.ingsis.clientserver.Message
import edu.austral.ingsis.clientserver.MessageAdapter
import edu.austral.ingsis.clientserver.serialization.Deserializer
import edu.austral.ingsis.clientserver.serialization.Serializer
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption.SO_KEEPALIVE
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.bytes.ByteArrayDecoder
import io.netty.handler.codec.bytes.ByteArrayEncoder
import java.net.SocketAddress

internal class NettyClient(
    private val address: SocketAddress,
    deserializer: Deserializer,
    private val serializer: Serializer,
    connectionListener: ClientConnectionListener?,
    messageListeners: Map<String, MessageAdapter<*>>,
) : Client {
    private val serverConnectionContainer = ServerConnectionContainer(connectionListener)
    private val clientChannelHandler = ClientChannelHandler(deserializer, serverConnectionContainer, messageListeners)

    private var workerGroup: EventLoopGroup? = null

    private var channel: Channel? = null

    override fun connect() {
        require(channel == null) { "Server already started" }

        workerGroup = NioEventLoopGroup()

        try {
            channel = createClientBootstrap(workerGroup!!)
                .connect(address).sync().channel()
        } catch (throwable: Throwable) {
            workerGroup?.shutdownGracefully()

            throw throwable
        }
    }

    override fun closeConnection() {
        try {
            channel?.close()
        } finally {
            workerGroup?.shutdownGracefully()
        }

        channel?.closeFuture()?.sync()
        channel = null
    }

    override fun <P : Any> send(message: Message<P>) {
        val serializedMessage = serializer.serialize(message)

        serverConnectionContainer.send(serializedMessage)
    }

    private fun createClientBootstrap(workerGroup: EventLoopGroup): Bootstrap {
        val bootstrap = Bootstrap()
        bootstrap.group(workerGroup)
        bootstrap.channel(NioSocketChannel::class.java)
        bootstrap.option(SO_KEEPALIVE, true)

        bootstrap.handler(object : ChannelInitializer<SocketChannel>() {
            @Throws(Exception::class)
            override fun initChannel(ch: SocketChannel) {
                ch.pipeline()
                    .addLast(ByteArrayEncoder())
                    .addLast(ByteArrayDecoder())
                    .addLast(clientChannelHandler)
            }
        })

        return bootstrap
    }
}
