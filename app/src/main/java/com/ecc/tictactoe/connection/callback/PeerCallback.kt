package com.ecc.tictactoe.connection.callback

interface PeerCallback {
    fun awaitingConnection(authenticationToken: String)
    fun connectionAccepted()
    fun connectionRejected()
}