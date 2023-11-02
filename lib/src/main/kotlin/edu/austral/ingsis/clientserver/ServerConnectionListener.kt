package edu.austral.ingsis.clientserver

interface ServerConnectionListener {
    fun handleClientConnection(clientId: String)

    fun handleClientConnectionClosed(clientId: String)
}
