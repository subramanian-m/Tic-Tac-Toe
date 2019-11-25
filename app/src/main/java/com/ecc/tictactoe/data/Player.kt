package com.ecc.tictactoe.data

data class Player(val endPointId: String, val name: String, val authenticationToken: String)

fun MutableList<Player>.removePlayer(endpointId: String) {
    this.forEach { player ->
        if (player.endPointId == endpointId) {
            this.remove(player)
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