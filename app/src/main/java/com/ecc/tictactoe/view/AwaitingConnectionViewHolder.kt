package com.ecc.tictactoe.view

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecc.tictactoe.R
import com.ecc.tictactoe.callback.AwaitingConnectionViewHolderCallback
import com.ecc.tictactoe.data.Player

class AwaitingConnectionViewHolder(itemView: View, private val callback: AwaitingConnectionViewHolderCallback) : RecyclerView.ViewHolder(itemView) {

    private val name: TextView = itemView.findViewById(R.id.connection_name)
    private val authenticationToken: TextView = itemView.findViewById(R.id.authentication_code)
    private var player: Player? = null

    init {
        itemView.findViewById<Button>(R.id.accept_connection).setOnClickListener {
            if (player != null) {
                callback.acceptConnection(player!!.endPointId)
            }
        }

        itemView.findViewById<Button>(R.id.reject_connection).setOnClickListener {
            if (player != null) {
                callback.rejectConnection(player!!.endPointId)
            }
        }
    }

    fun configure(player: Player) {
        this.player = player
        name.text = player.name
        authenticationToken.text = player.authenticationToken
    }
}