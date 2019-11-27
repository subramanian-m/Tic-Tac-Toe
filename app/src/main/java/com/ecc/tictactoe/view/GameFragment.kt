package com.ecc.tictactoe.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ecc.tictactoe.R
import com.ecc.tictactoe.data.Player

class GameFragment(
    val id: String,
    val player1: Player,
    val symbol1: Char,
    val player2: Player,
    val symbol2: Char
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.game_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.player_1_name).text = player1.name
        view.findViewById<TextView>(R.id.player_2_name).text = player2.name
        view.findViewById<TextView>(R.id.player_1_symbol).text = symbol1.toString()
        view.findViewById<TextView>(R.id.player_2_symbol).text = symbol2.toString()

    }
}