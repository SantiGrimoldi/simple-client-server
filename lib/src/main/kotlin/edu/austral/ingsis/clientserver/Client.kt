package edu.austral.ingsis.clientserver

interface Client {
    fun connect()

    fun closeConnection()

    fun <P : Any> send(message: Message<P>)
}
