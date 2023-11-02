package edu.austral.ingsis.clientserver.netty

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import edu.austral.ingsis.clientserver.Client
import edu.austral.ingsis.clientserver.ClientBuilder
import edu.austral.ingsis.clientserver.Message
import edu.austral.ingsis.clientserver.Server
import edu.austral.ingsis.clientserver.ServerBuilder
import edu.austral.ingsis.clientserver.netty.client.NettyClientBuilder
import edu.austral.ingsis.clientserver.netty.server.NettyServerBuilder
import edu.austral.ingsis.clientserver.util.MessageCollectorListener
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class NettyServerTransmissionTest {

    companion object {
        private const val HOST = "localhost"
        const val PORT = 10_000

        val ADDRESS = InetSocketAddress(HOST, PORT)

        private val DATA_MESSAGE_TYPE = "data-type"
        private val RAW_MESSAGE_TYPE = "raw-type"
    }

    data class Data(val field: String)

    // Collectors
    private val serverDataCollector = MessageCollectorListener<Data>()
    private val client1DataCollector = MessageCollectorListener<Data>()
    private val client2DataCollector = MessageCollectorListener<Data>()
    private val client3DataCollector = MessageCollectorListener<Data>()

    private val serverRawCollector = MessageCollectorListener<String>()
    private val client1RawCollector = MessageCollectorListener<String>()

    // Server
    private val server: Server = createServerBuilder()
        .addMessageListener(DATA_MESSAGE_TYPE, jacksonTypeRef(), serverDataCollector)
        .addMessageListener(RAW_MESSAGE_TYPE, jacksonTypeRef(), serverRawCollector)
        .build()

    // Clients
    private val client1: Client = createClientBuilder()
        .addMessageListener(DATA_MESSAGE_TYPE, jacksonTypeRef(), client1DataCollector)
        .addMessageListener(RAW_MESSAGE_TYPE, jacksonTypeRef(), client1RawCollector)
        .build()

    private val client2: Client = createClientBuilder()
        .addMessageListener(DATA_MESSAGE_TYPE, jacksonTypeRef(), client2DataCollector)
        .build()

    private val client3: Client = createClientBuilder()
        .addMessageListener(DATA_MESSAGE_TYPE, jacksonTypeRef(), client3DataCollector)
        .build()

    private fun createServerBuilder(): ServerBuilder =
        NettyServerBuilder.createDefault()
            .withPort(NettyServerConnectionTest.PORT)

    private fun createClientBuilder(): ClientBuilder =
        NettyClientBuilder.createDefault()
            .withAddress(ADDRESS)

    @BeforeEach
    fun init() {
        // Start server
        server.start()

        // Connect clients
        client1.connect()
        client2.connect()
        client3.connect()

        // Clear collectors
        serverDataCollector.clear()
        client1DataCollector.clear()
        client2DataCollector.clear()
        client3DataCollector.clear()
    }

    @AfterEach
    fun closeConnections() {
        client1.closeConnection()
        client2.closeConnection()
        client3.closeConnection()

        server.stop()
    }

    @Test
    fun `send string message from client to server`() {
        // Send message
        val message = Message(RAW_MESSAGE_TYPE, "Hello!")
        client1.send(message)

        // Waits for message to arrive
        Thread.sleep(200)

        assertEquals(1, serverRawCollector.messages.size)
        assertEquals(message.payload, serverRawCollector.messages.first())

        assertEquals(0, client1RawCollector.messages.size)
    }

    @Test
    fun `send object message from client to server`() {
        // Send message
        val message = Message(DATA_MESSAGE_TYPE, Data("Hello!"))
        client1.send(message)

        // Waits for message to arrive
        Thread.sleep(200)

        assertEquals(1, serverDataCollector.messages.size)
        assertEquals(message.payload, serverDataCollector.messages.first())

        assertEquals(0, client1DataCollector.messages.size)
    }

    @Test
    fun `send object broadcast to clients`() {
        // Send message
        val message = Message(DATA_MESSAGE_TYPE, Data("Hello!"))
        server.broadcast(message)

        // Waits for message to arrive
        Thread.sleep(200)

        assertEquals(0, serverDataCollector.messages.size)

        assertEquals(1, client1DataCollector.messages.size)
        assertEquals(message.payload, client1DataCollector.messages.first())
        assertEquals(1, client2DataCollector.messages.size)
        assertEquals(message.payload, client2DataCollector.messages.first())
        assertEquals(1, client3DataCollector.messages.size)
        assertEquals(message.payload, client3DataCollector.messages.first())
    }

    @Test
    fun `send several messages between client and sever`() {
        val message1 = Message(DATA_MESSAGE_TYPE, Data("message-1"))
        val message2 = Message(DATA_MESSAGE_TYPE, Data("message-2"))
        val message3 = Message(DATA_MESSAGE_TYPE, Data("message-3"))
        val message4 = Message(DATA_MESSAGE_TYPE, Data("message-4"))

        // Send message
        client1.send(message1)
        server.broadcast(message2)
        client1.send(message3)
        server.broadcast(message4)

        // Waits for message to arrive
        Thread.sleep(300)

        assertEquals(2, serverDataCollector.messages.size)
        assertContains(serverDataCollector.messages, message1.payload)
        assertContains(serverDataCollector.messages, message3.payload)

        assertEquals(2, client1DataCollector.messages.size)
        assertContains(client1DataCollector.messages, message2.payload)
        assertContains(client1DataCollector.messages, message4.payload)
    }
}
