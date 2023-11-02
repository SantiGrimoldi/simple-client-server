package edu.austral.ingsis.clientserver

interface Server {
    fun start()

    fun stop()

    fun <P : Any> sendMessage(clientId: String, message: Message<P>)

    fun <P : Any> broadcast(message: Message<P>)
}
