package com.ecc.tictactoe.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecc.tictactoe.R
import com.ecc.tictactoe.callback.AwaitingConnectionViewHolderCallback
import com.ecc.tictactoe.data.Player

class AwaitingConnectionsAdapter(var players: List<Player>, private val callback: AwaitingConnectionViewHolderCallback) :
    RecyclerView.Adapter<AwaitingConnectionViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AwaitingConnectionViewHolder {
        return AwaitingConnectionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.awaiting_connections_list_item, parent,
                false
            ), callback
        )
    }

    override fun getItemCount(): Int {
        return players.count()
    }

    override fun onBindViewHolder(holder: AwaitingConnectionViewHolder, position: Int) {
        holder.configure(players[position])
    }
}