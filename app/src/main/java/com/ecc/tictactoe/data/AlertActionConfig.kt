package com.ecc.tictactoe.data

import android.content.DialogInterface

data class AlertActionConfig(
    val type: Int,
    val text: String,
    val listener: DialogInterface.OnClickListener
)