package com.ecc.tictactoe

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecc.tictactoe.callback.AwaitingConnectionViewHolderCallback
import com.ecc.tictactoe.connection.Host
import com.ecc.tictactoe.connection.Peer
import com.ecc.tictactoe.connection.callback.GameCallback
import com.ecc.tictactoe.connection.callback.HostCallback
import com.ecc.tictactoe.connection.callback.PeerCallback
import com.ecc.tictactoe.connection.data.PayloadData
import com.ecc.tictactoe.data.AlertActionConfig
import com.ecc.tictactoe.data.GameMove
import com.ecc.tictactoe.data.GameTurn
import com.ecc.tictactoe.data.InitiateGame
import com.ecc.tictactoe.view.AcceptedConnectionsAdapter
import com.ecc.tictactoe.view.AwaitingConnectionsAdapter
import com.ecc.tictactoe.view.GameFragment
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var connectionsClient: ConnectionsClient
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

    private lateinit var host: Host
    private lateinit var peer: Peer
    private var gson = Gson()
    lateinit var gameFragment: GameFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.connectionsClient = Nearby.getConnectionsClient(this)

        action_host.setOnClickListener {
            if (!isNameAvailable()) {
                return@setOnClickListener
            }

            val awaitingConnectionsAdapter =
                AwaitingConnectionsAdapter(listOf(), object : AwaitingConnectionViewHolderCallback {
                    override fun acceptConnection(endpointId: String) {
                        host.accept(endpointId)
                    }

                    override fun rejectConnection(endpointId: String) {
                        host.reject(endpointId)
                    }
                })
            val acceptedConnectionsAdapter = AcceptedConnectionsAdapter(listOf())

            val name = player_name.text.toString()
            host = Host(this, name, object : HostCallback {
                override fun payloadReceived(payload: String) {
                    val payloadData = gson.fromJson<PayloadData>(payload, PayloadData::class.java)
                    if (payloadData.type == "_move_") {
                        val gameMove =
                            gson.fromJson<GameMove>(payloadData.value, GameMove::class.java)
                        gameFragment.move(gameMove.endpointId, gameMove.row, gameMove.column)

                        val gameTurn = GameTurn(host.acceptedConnections[0].endPointId)
                        val payloadGameTurnData =
                            PayloadData("_gameTurn_", gson.toJson(gameTurn))
                        host.sendPayload(
                            host.acceptedConnections[1].endPointId,
                            gson.toJson(payloadGameTurnData)
                        )
                        gameFragment.isInteractive = true
                    }
                }

            })
            host.awaitingConnectionsObservable.observe(this, Observer { players ->
                awaitingConnectionsAdapter.players = players
                awaitingConnectionsAdapter.notifyDataSetChanged()
            })
            host.acceptedConnectionsObservable.observe(this, Observer { players ->
                acceptedConnectionsAdapter.players = players
                acceptedConnectionsAdapter.notifyDataSetChanged()

                if (players.count() == 2) {
                    start_game.isEnabled = true
                    host.rejectAll()
                }
            })
            val code = host.advertise()
            host_code.text = code

            val awaitingConnectionsLayoutManager = LinearLayoutManager(this)
            awaitingConnectionsLayoutManager.orientation = RecyclerView.VERTICAL
            awaiting_connections.layoutManager = awaitingConnectionsLayoutManager
            awaiting_connections.adapter = awaitingConnectionsAdapter

            val acceptedConnectionsLayoutManager = LinearLayoutManager(this)
            acceptedConnectionsLayoutManager.orientation = RecyclerView.VERTICAL
            accepted_connection.layoutManager = acceptedConnectionsLayoutManager
            accepted_connection.adapter = acceptedConnectionsAdapter

            it.isEnabled = false
            action_join.isEnabled = false
            code_to_join.isEnabled = false
        }

        action_join.setOnClickListener {
            if (!isNameAvailable()) {
                return@setOnClickListener
            }

            val hostCode = code_to_join.text.toString()
            if (hostCode == "") {
                val title = "Forgot Code?"
                val message = "Please enter the code provided by host and then click Join"
                val neutralButton = AlertActionConfig(
                    AlertDialog.BUTTON_NEUTRAL,
                    "OK",
                    DialogInterface.OnClickListener { dialog, _ ->
                        dialog.dismiss()
                    })
                showDialog(title, message, neutralButton)
                return@setOnClickListener
            }

            val name = player_name.text.toString()
            peer = Peer(this, name, object : PeerCallback {
                override fun awaitingConnection(authenticationToken: String) {
                    peer_status.text =
                        "Waiting for the connection to be accepted. Authentication Token: $authenticationToken"
                }

                override fun connectionAccepted() {
                    peer_status.text = "Connection Accepted"
                }

                override fun connectionRejected() {
                    peer_status.text = "Connection Rejected"
                }

                override fun payloadReceived(payload: String) {
                    val payloadData = gson.fromJson<PayloadData>(payload, PayloadData::class.java)
                    if (payloadData.type == "_initiate_") {
                        val initiateGame =
                            gson.fromJson<InitiateGame>(payloadData.value, InitiateGame::class.java)

                        gameFragment = GameFragment(
                            peer.selfEndpointId,
                            initiateGame.player1,
                            initiateGame.symbol1,
                            initiateGame.player2,
                            initiateGame.symbol2,
                            object : GameCallback {
                                override fun gameMove(
                                    id: String,
                                    row: Int,
                                    column: Int,
                                    status: GameFragment.STATUS
                                ) {
                                    val gameMove = GameMove(id, row, column)
                                    val gameMovePayload =
                                        PayloadData("_move_", gson.toJson(gameMove))
                                    peer.sendPayload(
                                        peer.host.endPointId,
                                        gson.toJson(gameMovePayload)
                                    )

                                    if (status == GameFragment.STATUS.WIN) {
                                        if (id == peer.acceptedConnections[1].endPointId) {
                                            val title = "Congratulations.!"
                                            val message = "Congratulations.! You won.!"

                                            val positiveButton = AlertActionConfig(
                                                AlertDialog.BUTTON_POSITIVE,
                                                "OK",
                                                DialogInterface.OnClickListener { dialog, _ ->
                                                    dialog.dismiss()
                                                })
                                            showDialog(title, message, positiveButton)

                                        } else {
                                            val title = "OOPS.!"
                                            val message = "OOPS.! You lost.!"

                                            val positiveButton = AlertActionConfig(
                                                AlertDialog.BUTTON_POSITIVE,
                                                "OK",
                                                DialogInterface.OnClickListener { dialog, _ ->
                                                    dialog.dismiss()
                                                })
                                            showDialog(title, message, positiveButton)
                                        }
                                    } else if (status == GameFragment.STATUS.DRAW) {
                                        val title = "-----------.!"
                                        val message = "----------.! It's a tie.!"

                                        val positiveButton = AlertActionConfig(
                                            AlertDialog.BUTTON_POSITIVE,
                                            "OK",
                                            DialogInterface.OnClickListener { dialog, _ ->
                                                dialog.dismiss()
                                            })
                                        showDialog(title, message, positiveButton)
                                    }

                                }

                            }
                        )
                        val transaction = supportFragmentManager.beginTransaction()
                        transaction.add(R.id.game_container, gameFragment)
                        transaction.commit()
                    } else if (payloadData.type == "_gameTurn_") {
                        val gameTurn =
                            gson.fromJson<GameTurn>(payloadData.value, GameTurn::class.java)
                        if (gameTurn.endpointId == gameFragment.id) {
                            gameFragment.isInteractive = true
                        }
                    } else if (payloadData.type == "_move_") {
                        val gameMove =
                            gson.fromJson<GameMove>(payloadData.value, GameMove::class.java)
                        gameFragment.move(gameMove.endpointId, gameMove.row, gameMove.column)
                    }
                }

            })
            peer.acceptedConnectionsObservable.observe(this, Observer { players ->
                players.forEach { player ->
                    Log.e("Player ", player.toString())
                }
            })

            peer.discover(hostCode)
            peer_status.text = "Discovering"

            it.isEnabled = false
            code_to_join.isEnabled = false
            action_host.isEnabled = false
        }

        grant_permission.setOnClickListener {
            requestPermissions(this)
        }

        submit_name.setOnClickListener {
            if (player_name.text.toString() == "") {
                val title = "Require Name"
                val message = "Please enter your name before submitting"
                val neutralButton = AlertActionConfig(
                    AlertDialog.BUTTON_NEUTRAL,
                    "OK",
                    DialogInterface.OnClickListener { dialog, _ ->
                        dialog.dismiss()
                    })
                showDialog(title, message, neutralButton)
            } else {
                player_name.isEnabled = false
                it.isEnabled = false
            }
        }

        start_game.setOnClickListener {
            it.isEnabled = false

            val randomValue = (-1..2).shuffled().first()
            val player1Symbol: Char = if (randomValue == 0) 'X' else 'O'
            val player2Symbol: Char = if (randomValue == 0) 'O' else 'X'

            host.stop()
            val connectedPlayers = host.acceptedConnections
            if (connectedPlayers.count() == 2) {
                val gameEvent = InitiateGame(
                    connectedPlayers[0],
                    player1Symbol,
                    connectedPlayers[1],
                    player2Symbol
                )
                val payloadData = PayloadData("_initiate_", gson.toJson(gameEvent))
                host.sendPayload(connectedPlayers[1].endPointId, gson.toJson(payloadData))

                gameFragment = GameFragment(
                    connectedPlayers[0].endPointId,
                    connectedPlayers[0],
                    player1Symbol,
                    connectedPlayers[1],
                    player2Symbol,
                    object : GameCallback {
                        override fun gameMove(
                            id: String,
                            row: Int,
                            column: Int,
                            status: GameFragment.STATUS
                        ) {
                            val gameMove = GameMove(id, row, column)
                            val gameMovePayload =
                                PayloadData("_move_", gson.toJson(gameMove))
                            host.sendPayload(
                                host.acceptedConnections[1].endPointId,
                                gson.toJson(gameMovePayload)
                            )

                            val gameTurn = GameTurn(host.acceptedConnections[1].endPointId)
                            val payloadGameTurnData =
                                PayloadData("_gameTurn_", gson.toJson(gameTurn))
                            host.sendPayload(
                                connectedPlayers[1].endPointId,
                                gson.toJson(payloadGameTurnData)
                            )

                            if (status == GameFragment.STATUS.WIN) {
                                if (id == host.acceptedConnections[0].endPointId) {
                                    val title = "Congratulations.!"
                                    val message = "Congratulations.! You won.!"

                                    val positiveButton = AlertActionConfig(
                                        AlertDialog.BUTTON_POSITIVE,
                                        "OK",
                                        DialogInterface.OnClickListener { dialog, _ ->
                                            dialog.dismiss()
                                        })
                                    showDialog(title, message, positiveButton)

                                } else {
                                    val title = "OOPS.!"
                                    val message = "OOPS.! You lost.!"

                                    val positiveButton = AlertActionConfig(
                                        AlertDialog.BUTTON_POSITIVE,
                                        "OK",
                                        DialogInterface.OnClickListener { dialog, _ ->
                                            dialog.dismiss()
                                        })
                                    showDialog(title, message, positiveButton)
                                }
                            } else if (status == GameFragment.STATUS.DRAW) {
                                val title = "-----------.!"
                                val message = "----------.! It's a tie.!"

                                val positiveButton = AlertActionConfig(
                                    AlertDialog.BUTTON_POSITIVE,
                                    "OK",
                                    DialogInterface.OnClickListener { dialog, _ ->
                                        dialog.dismiss()
                                    })
                                showDialog(title, message, positiveButton)
                            }
                        }
                    }
                )
                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(R.id.game_container, gameFragment)
                transaction.commit()

                val gameTurn = if (player1Symbol == 'X') {
                    GameTurn(connectedPlayers[0].endPointId)
                } else {
                    GameTurn(connectedPlayers[1].endPointId)
                }
                val payloadGameTurnData = PayloadData("_gameTurn_", gson.toJson(gameTurn))
                host.sendPayload(connectedPlayers[1].endPointId, gson.toJson(payloadGameTurnData))
                if (gameTurn.endpointId == "_endpointId_") {
                    gameFragment.isInteractive = true
                }
            }
        }

    }

    private fun isNameAvailable(): Boolean {
        if (player_name.isEnabled) {
            val title = "Require Name"
            val message = "Please enter and submit your name before continuing"
            val neutralButton = AlertActionConfig(
                AlertDialog.BUTTON_NEUTRAL,
                "OK",
                DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                })
            showDialog(title, message, neutralButton)
        }
        return !player_name.isEnabled
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun requestPermissions(context: Context) {
        REQUIRED_PERMISSIONS.forEach { permission ->
            if (shouldShowRequestPermissionRationale(permission)) {
                val title = "Require Permission"
                val message =
                    "We need the asked permission in oreder to connect you with your peer. Please grant permissions to continue"
                val neutralButton = AlertActionConfig(
                    AlertDialog.BUTTON_NEUTRAL,
                    "OK",
                    DialogInterface.OnClickListener { dialog, _ ->
                        dialog.dismiss()
                        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
                    })
                showDialog(title, message, neutralButton)
                return
            } else if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val title = "Require Permission"
                val message =
                    "You have forcefully denied permissions. We need the asked permission in oreder to connect you with your peer. Please grant permissions to continue"
                val neutralButton = AlertActionConfig(
                    AlertDialog.BUTTON_NEUTRAL,
                    "OK",
                    DialogInterface.OnClickListener { dialog, _ ->
                        dialog.dismiss()
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    })
                showDialog(title, message, neutralButton)
            }
        }
        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
    }

    private fun showDialog(
        title: String,
        message: String,
        vararg alertActionConfigs: AlertActionConfig
    ) {
        val alertDialog: AlertDialog = AlertDialog.Builder(this@MainActivity).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)

        alertActionConfigs.forEach { alertActionConfig ->
            alertDialog.setButton(
                alertActionConfig.type,
                alertActionConfig.text,
                alertActionConfig.listener
            )
        }
        alertDialog.show()
    }

    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(this)
        } else if (grant_permission_panel.visibility == View.VISIBLE) {
            grant_permission_panel.visibility = View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            grantResults.forEach { grantResult ->
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    grant_permission_panel.visibility = View.VISIBLE
                    return
                }
            }
        }
    }

}

