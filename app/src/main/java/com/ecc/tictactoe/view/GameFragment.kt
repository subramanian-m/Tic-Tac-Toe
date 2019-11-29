package com.ecc.tictactoe.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ecc.tictactoe.R
import com.ecc.tictactoe.connection.callback.GameCallback
import com.ecc.tictactoe.data.Player
import kotlinx.android.synthetic.main.game_fragment.*

class GameFragment(
    val id: String,
    private val player1: Player,
    private val symbol1: Char,
    private val player2: Player,
    private val symbol2: Char,
    private val gameCallback: GameCallback
) : Fragment() {

    enum class STATUS {
        WIN, DRAW, NONE
    }

    var isInteractive: Boolean = false

    var gameBoard = arrayOf(intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0))

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

        row0_col0.setOnClickListener { tileClicked(0, 0) }
        row0_col1.setOnClickListener { tileClicked(0, 1) }
        row0_col2.setOnClickListener { tileClicked(0, 2) }
        row1_col0.setOnClickListener { tileClicked(1, 0) }
        row1_col1.setOnClickListener { tileClicked(1, 1) }
        row1_col2.setOnClickListener { tileClicked(1, 2) }
        row2_col0.setOnClickListener { tileClicked(2, 0) }
        row2_col1.setOnClickListener { tileClicked(2, 1) }
        row2_col2.setOnClickListener { tileClicked(2, 2) }

    }

    private fun tileClicked(row: Int, column: Int) {
        if (!isInteractive || gameBoard[row][column] != 0) {
            return
        }
        gameCallback.gameMove(id, row, column, move(id, row, column))
        isInteractive = false
    }

    fun move(playerId: String, row: Int, column: Int): STATUS {
        val ids = arrayOf(
            intArrayOf(R.id.row0_col0, R.id.row0_col1, R.id.row0_col2),
            intArrayOf(R.id.row1_col0, R.id.row1_col1, R.id.row1_col2),
            intArrayOf(R.id.row2_col0, R.id.row2_col1, R.id.row2_col2)
        )

        if (view == null) {
            return STATUS.NONE
        }

        val imageView = view!!.findViewById<ImageView>(ids[row][column])
        when (if (player1.endPointId == playerId) symbol1 else symbol2) {
            'X' -> {
                (imageView as ImageView).setBackgroundResource(R.drawable.ic_x)
                gameBoard[row][column] = 1
                return isPlayerWin(1)
            }
            'O' -> {
                (imageView as ImageView).setBackgroundResource(R.drawable.ic_o)
                gameBoard[row][column] = -1
                return isPlayerWin(-1)
            }
        }
        return STATUS.NONE
    }

    private fun isPlayerWin(player: Int): STATUS {
        if (gameBoard[0][0] + gameBoard[1][0] + gameBoard[2][0] == player * 3 || gameBoard[0][0] + gameBoard[0][1] + gameBoard[0][2] == player * 3 || gameBoard[0][0] + gameBoard[1][1] + gameBoard[2][2] == player * 3 || gameBoard[1][0] + gameBoard[1][1] + gameBoard[1][2] == player * 3 || gameBoard[0][1] + gameBoard[1][1] + gameBoard[2][1] == player * 3 || gameBoard[2][0] + gameBoard[2][1] + gameBoard[2][2] == player * 3 || gameBoard[0][2] + gameBoard[1][2] + gameBoard[2][2] == player * 3 || gameBoard[2][0] + gameBoard[1][1] + gameBoard[0][2] == player * 3) {
            return STATUS.WIN

        } else if (gameBoard[0][0] != 0 && gameBoard[1][0] != 0 && gameBoard[2][0] != 0 && gameBoard[0][0] != 0 && gameBoard[0][1] != 0 && gameBoard[0][2] != 0 && gameBoard[0][0] != 0 && gameBoard[1][1] != 0 && gameBoard[2][2] != 0 && gameBoard[1][0] != 0 && gameBoard[1][1] != 0 && gameBoard[1][2] != 0 && gameBoard[0][1] != 0 && gameBoard[1][1] != 0 && gameBoard[2][1] != 0 && gameBoard[2][0] != 0 && gameBoard[2][1] != 0 && gameBoard[2][2] != 0 && gameBoard[0][2] != 0 && gameBoard[1][2] != 0 && gameBoard[2][2] != 0 && gameBoard[2][0] != 0 && gameBoard[1][1] != 0 && gameBoard[0][2] != 0) {
            return STATUS.DRAW
        }

        return STATUS.NONE
    }
}