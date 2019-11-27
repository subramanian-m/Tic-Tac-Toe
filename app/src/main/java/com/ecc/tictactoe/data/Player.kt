package com.ecc.tictactoe.data

import com.google.gson.annotations.SerializedName

data class Player(
    @SerializedName("endpointId") val endPointId: String,
    @SerializedName("name") val name: String,
    @SerializedName("authenticationToken") val authenticationToken: String
)

fun MutableList<Player>.removePlayer(endpointId: String) {
    for (i in 0 until this.count()) {
        val player = this[i]
        if (player.endPointId == endpointId) {
            this.removeAt(i)
            return
        }
    }
}

fun List<Player>.containsPlayer(endpointId: String): Boolean {
    this.forEach { player ->
        if (player.endPointId == endpointId) {
            return true
        }
    }
    return false
}

fun List<Player>.fetchPlayer(endpointId: String): Player? {
    this.forEach { player ->
        if (player.endPointId == endpointId) {
            return player
        }
    }
    return null
}