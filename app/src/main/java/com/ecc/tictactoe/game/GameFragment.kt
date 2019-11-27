package com.ecc.tictactoe.game

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.ecc.tictactoe.R
import kotlinx.android.synthetic.main.fragmet_game.*

class GameFragment : Fragment() {

    val playerX = 1
    val playerO = -1
    var isplayerXchance = true
    var count: Int = 0
    val board = arrayOf(intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), intArrayOf(0, 0, 0))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmet_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        claearBoard()
        createBoard()
    }

    private fun claearBoard() {
        column0.removeAllViews()
        column1.removeAllViews()
        column2.removeAllViews()
    }

    private fun createBoard() {
        for (row in 0..2) {
            for (column in 0..2) {
                val imageView = ImageView(context)
                imageView.tag = "rc$row$column"
                imageView.setBackgroundColor(Color.GRAY)
                val layoutParams = LinearLayout.LayoutParams(200, 200)
                layoutParams.setMargins(5, 5, 5, 5)
                imageView.layoutParams = layoutParams
                imageView.setOnClickListener {
                    playGame(imageView.tag as String)
                }
                if (column == 0) {
                    column0.addView(imageView)
                }
                if (column == 1) {
                    column1.addView(imageView)
                }
                if (column == 2) {
                    column2.addView(imageView)
                }
            }
        }

    }

    private fun playGame(tag: String) {
        val row = tag.substring(2, 3).toInt()
        val column = tag.substring(3).toInt()
        if (board[row][column] != 0) {
            return
        }
        if (isplayerXchance) {
            board[row][column] = playerX
            updateUI(tag, playerX)
            isPlayerWin(playerX)
        } else {
            board[row][column] = playerO
            updateUI(tag, playerO)
            isPlayerWin(playerO)
        }
        isplayerXchance = !isplayerXchance
    }

    private fun isPlayerWin(player: Int) {
        if (board[0][0] + board[1][0] + board[2][0] == player * 3 || board[0][0] + board[0][1] + board[0][2] == player * 3 || board[0][0] + board[1][1] + board[2][2] == player * 3 || board[1][0] + board[1][1] + board[1][2] == player * 3 || board[0][1] + board[1][1] + board[2][1] == player * 3 || board[2][0] + board[2][1] + board[2][2] == player * 3 || board[0][2] + board[1][2] + board[2][2] == player * 3 || board[2][0] + board[1][1] + board[0][2] == player * 3) {
            when (player) {
                playerX -> {
                    win_status.text = "Winner is Player X"
                }
                playerO -> {
                    win_status.text = "Winner is Player O"
                }
            }
        } else if (board[0][0] != 0 && board[1][0] != 0 && board[2][0] != 0 && board[0][0] != 0 && board[0][1] != 0 && board[0][2] != 0 && board[0][0] != 0 && board[1][1] != 0 && board[2][2] != 0 && board[1][0] != 0 && board[1][1] != 0 && board[1][2] != 0 && board[0][1] != 0 && board[1][1] != 0 && board[2][1] != 0 && board[2][0] != 0 && board[2][1] != 0 && board[2][2] != 0 && board[0][2] != 0 && board[1][2] != 0 && board[2][2] != 0 && board[2][0] != 0 && board[1][1] != 0 && board[0][2] != 0) {
            win_status.text = "Draw Match"
        }

    }

    private fun updateUI(tag: String, player: Int) {
        val imageView = view!!.findViewWithTag<ImageView>(tag)
        when (player) {
            playerX -> {
                imageView.setBackgroundResource(R.drawable.ic_x)
            }
            playerO -> {
                imageView.setBackgroundResource(R.drawable.ic_o)
            }
        }
    }


}