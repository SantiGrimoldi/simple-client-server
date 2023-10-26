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
import org.apache.mina.transport.socket.nio.NioSocketConnector
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit.SECONDS

class SimpleClient(
    private val serverAddress: InetSocketAddress,
    private val serializer: Serializer,
    private val deserializer: Deserializer,
) : Disposable {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val listenerContainer = MessageListenerContainer(logger, deserializer)

    private val session: IoSession
    private val connector = NioSocketConnector()

    init {

        connector.getFilterChain()
            .addLast("logger", LoggingFilter())
        connector.getFilterChain()
            .addLast("codec", ProtocolCodecFilter(TextLineCodecFactory(Charset.defaultCharset())))

        connector.handler = object : IoHandlerAdapter() {
            override fun messageReceived(session: IoSession?, message: Any?) {
                listenerContainer.handleMessage(message.toString())
            }

            override fun sessionClosed(session: IoSession) {
                dispose()
            }
        }

        val future = connector.connect(serverAddress)
        future.await(10, SECONDS)
        session = future.session
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

    fun <P : Any> sendMessage(message: Message<P>) {
        if (session.isActive) {
            val stringMessage = String(serializer.serialize(message), Charset.defaultCharset())
            session.write(stringMessage)
        } else {
            logger.warn("Client is down: ${session.remoteAddress}")
        }
    }

    override fun dispose() {
        if (session.isConnected && !session.isClosing) {
            session.closeFuture.await(1, SECONDS)
        }

        if (connector.isActive && !connector.isDisposing) {
            connector.dispose(true)
        }
    }
}
