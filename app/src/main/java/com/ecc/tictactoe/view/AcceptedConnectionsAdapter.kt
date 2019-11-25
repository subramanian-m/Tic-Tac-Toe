package com.ecc.tictactoe.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecc.tictactoe.R
import com.ecc.tictactoe.data.Player

class AcceptedConnectionsAdapter(var players: List<Player>) :
    RecyclerView.Adapter<AcceptedConnectionViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AcceptedConnectionViewHolder {
        return AcceptedConnectionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.accepted_connections_list_item, parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return players.count()
    }

    override fun onBindViewHolder(holder: AcceptedConnectionViewHolder, position: Int) {
        holder.configure(players[position])
    }

}