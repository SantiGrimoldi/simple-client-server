package edu.austral.ingsis.clientserver

interface ClientConnectionListener {
    fun handleConnection()

    fun handleConnectionClosed()
}
