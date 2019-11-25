package com.ecc.tictactoe.view

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecc.tictactoe.R
import com.ecc.tictactoe.data.Player

class AcceptedConnectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val name: TextView = itemView.findViewById(R.id.player_name)

    fun configure(player: Player) {
        this.name.text = player.name
    }
}