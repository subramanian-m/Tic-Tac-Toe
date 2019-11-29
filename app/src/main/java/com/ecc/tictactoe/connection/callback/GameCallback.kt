package com.ecc.tictactoe.connection.callback

import com.ecc.tictactoe.view.GameFragment

interface GameCallback {

    fun gameMove(
        id: String,
        row: Int,
        column: Int,
        status: GameFragment.STATUS
    )
}