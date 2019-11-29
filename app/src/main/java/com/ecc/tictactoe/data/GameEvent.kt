package com.ecc.tictactoe.data

import com.google.gson.annotations.SerializedName

data class InitiateGame(
    @SerializedName("player1") val player1: Player,
    @SerializedName("symbol1") val symbol1: Char,
    @SerializedName("player2") val player2: Player,
    @SerializedName("symbol2") val symbol2: Char
)

data class GameTurn(
    @SerializedName("turn_for") val endpointId: String
)

data class GameMove(
    @SerializedName("endpointId") val endpointId: String,
    @SerializedName("row") val row: Int,
    @SerializedName("column") val column: Int
)