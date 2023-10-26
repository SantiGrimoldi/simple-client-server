package edu.austral.ingsis.clientserver.util

import edu.austral.ingsis.clientserver.Message
import edu.austral.ingsis.clientserver.MessageListener

class MessageCollectorListener<P : Any> : MessageListener<P> {
    val messages = mutableListOf<P>()

    override fun handleMessage(message: Message<P>) {
        messages.add(message.payload)
    }

    fun clear() {
        messages.removeAll { true }
    }
}
