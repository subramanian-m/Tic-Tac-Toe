package com.ecc.tictactoe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecc.tictactoe.data.Player

class ConnectedPlayerAdapter :
    RecyclerView.Adapter<ConnectedPlayerAdapter.ConnectedPlayerViewHolder>() {

    private var players: List<Player> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectedPlayerViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.player_data_holder, parent, false)
        return ConnectedPlayerViewHolder(view)
    }

    override fun getItemCount(): Int = this.players.size


    override fun onBindViewHolder(holder: ConnectedPlayerViewHolder, position: Int) {
        holder.playerName.text = players[position].name
    }

    inner class ConnectedPlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerName: TextView = itemView.findViewById(R.id.player_name)
    }

    fun updatePlayers(playersList: List<Player>) {
        this.players = playersList
        notifyDataSetChanged()
    }

}
