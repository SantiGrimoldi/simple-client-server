package edu.austral.ingsis.clientserver

interface MessageListener<P : Any> {
    fun handleMessage(message: Message<P>)
}
