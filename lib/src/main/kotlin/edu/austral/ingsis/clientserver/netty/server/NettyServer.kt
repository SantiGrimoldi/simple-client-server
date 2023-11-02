package edu.austral.ingsis.clientserver.netty.server

import edu.austral.ingsis.clientserver.Message
import edu.austral.ingsis.clientserver.MessageAdapter
import edu.austral.ingsis.clientserver.Server
import edu.austral.ingsis.clientserver.ServerConnectionListener
import edu.austral.ingsis.clientserver.serialization.Deserializer
import edu.austral.ingsis.clientserver.serialization.Serializer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption.SO_BACKLOG
import io.netty.channel.ChannelOption.SO_KEEPALIVE
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.bytes.ByteArrayDecoder
import io.netty.handler.codec.bytes.ByteArrayEncoder

internal class NettyServer(
    private val port: Int,
    deserializer: Deserializer,
    private val serializer: Serializer,
    connectionListener: ServerConnectionListener?,
    messageListeners: Map<String, MessageAdapter<*>>,
) : Server {
    private val clientConnectionsContainer = ClientConnectionsContainer(connectionListener)
    private val channelHandler = ServerChannelHandler(deserializer, clientConnectionsContainer, messageListeners)

    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null

    private var channel: Channel? = null

    override fun start() {
        require(channel == null) { "Server already started" }

        bossGroup = NioEventLoopGroup()
        workerGroup = NioEventLoopGroup()

        try {
            channel = createServerBootstrap(bossGroup!!, workerGroup!!)
                .bind(port).sync().channel()
        } catch (throwable: Throwable) {
            bossGroup?.shutdownGracefully()
            workerGroup?.shutdownGracefully()

            throw throwable
        }
    }

    override fun stop() {
        requireNotNull(channel) { "Server is not started" }

        try {
            channel?.close()
        } finally {
            workerGroup?.shutdownGracefully()
            bossGroup?.shutdownGracefully()
        }

        channel?.closeFuture()?.sync()
        channel = null
    }

    override fun <P : Any> sendMessage(clientId: String, message: Message<P>) {
        val serializedMessage = serializer.serialize(message)

        clientConnectionsContainer.send(clientId, serializedMessage)
    }

    override fun <P : Any> broadcast(message: Message<P>) {
        val serializedMessage = serializer.serialize(message)

        clientConnectionsContainer.broadcast(serializedMessage)
    }

    private fun createServerBootstrap(bossGroup: EventLoopGroup, workerGroup: EventLoopGroup): ServerBootstrap {
        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java) // (3)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline()
                        .addLast(LengthFieldPrepender(4), ByteArrayEncoder())
                        .addLast(LengthFieldBasedFrameDecoder(1_048_576, 0, 4, 0, 4), ByteArrayDecoder())
                        .addLast(channelHandler)
                }
            })
            .option(SO_BACKLOG, 128)
            .childOption(SO_KEEPALIVE, true)

        return bootstrap
    }
}
