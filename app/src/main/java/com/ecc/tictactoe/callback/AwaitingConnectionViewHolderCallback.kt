package com.ecc.tictactoe.callback

interface AwaitingConnectionViewHolderCallback {
    fun acceptConnection(endpointId: String)
    fun rejectConnection(endpointId: String)
}