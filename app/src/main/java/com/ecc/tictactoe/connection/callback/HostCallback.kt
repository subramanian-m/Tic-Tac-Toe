package com.ecc.tictactoe.connection.callback

interface HostCallback {

    fun payloadReceived(payload: String)
}