package edu.austral.ingsis.clientserver

import edu.austral.ingsis.clientserver.serialization.json.JsonDeserializer
import edu.austral.ingsis.clientserver.serialization.json.JsonSerializer
import java.net.InetSocketAddress

class ClientServerFactory {
    private val serializer = JsonSerializer()
    private val deserializer = JsonDeserializer()

    fun createDefaultServer(port: Int) = SimpleServer(port, serializer, deserializer)

    fun createDefaultClient(address: InetSocketAddress) = SimpleClient(address, serializer, deserializer)
}
