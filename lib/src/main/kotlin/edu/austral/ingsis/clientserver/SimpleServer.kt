package edu.austral.ingsis.clientserver

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import edu.austral.ingsis.clientserver.serialization.Deserializer
import edu.austral.ingsis.clientserver.serialization.Serializer
import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession
import org.apache.mina.filter.codec.ProtocolCodecFilter
import org.apache.mina.filter.codec.textline.TextLineCodecFactory
import org.apache.mina.filter.logging.LoggingFilter
import org.apache.mina.transport.socket.nio.NioSocketAcceptor
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit.SECONDS

class SimpleServer(
    private val port: Int,
    private val serializer: Serializer,
    private val deserializer: Deserializer,
) : Disposable {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val listenerContainer = MessageListenerContainer(logger, deserializer)

    private val clients by lazy { mutableListOf<IoSession>() }

    private val acceptor = NioSocketAcceptor()

    init {
        acceptor.getFilterChain()
            .addLast("logger", LoggingFilter())
        acceptor.getFilterChain()
            .addLast("codec", ProtocolCodecFilter(TextLineCodecFactory(Charset.defaultCharset())))

        acceptor.handler = object : IoHandlerAdapter() {
            override fun sessionCreated(session: IoSession) = registerClient(session)

            override fun sessionClosed(session: IoSession) = unregisterClient(session)

            override fun messageReceived(session: IoSession, message: Any) =
                listenerContainer.handleMessage(message.toString())
        }

        acceptor.bind(InetSocketAddress(port))
    }

    private fun registerClient(session: IoSession) {
        clients.add(session)
    }

    private fun unregisterClient(session: IoSession) {
        clients.remove(session)
    }

    inline fun <reified P : Any> setListenerFor(messageType: String, listener: MessageListener<P>) {
        setListenerFor(messageType, jacksonTypeRef<Message<P>>(), listener)
    }

    fun <P : Any> setListenerFor(
        messageType: String,
        messageTypeReference: TypeReference<Message<P>>,
        listener: MessageListener<P>,
    ) {
        listenerContainer.setListenerFor(messageType, messageTypeReference, listener)
    }

    fun unsetListenerFor(messageType: String) {
        listenerContainer.unsetListenerFor(messageType)
    }

    fun <P : Any> broadcast(message: Message<P>) {
        clients.forEach {
            if (it.isActive) {
                val stringMessage: String = String(serializer.serialize(message), Charset.defaultCharset())
                it.write(stringMessage)
            } else {
                logger.warn("Client is down: ${it.remoteAddress}")
            }
        }
    }

    override fun dispose() {
        clients.forEach {
            if (it.isConnected && !it.isClosing) {
                it.closeFuture.await(1, SECONDS)
            }
        }

        Thread.sleep(50)

        if (acceptor.isActive && !acceptor.isDisposing) {
            acceptor.dispose(true)
        }
    }
}
