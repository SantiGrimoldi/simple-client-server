package edu.austral.ingsis.clientserver

data class Message<P : Any>(val type: String, val payload: P)
