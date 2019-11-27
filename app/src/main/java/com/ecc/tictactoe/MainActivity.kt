package com.ecc.tictactoe

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.ecc.tictactoe.connection.callback.PeerCallback
import com.ecc.tictactoe.data.AlertActionConfig
import com.ecc.tictactoe.game.GameFragment
import com.ecc.tictactoe.view.AcceptedConnectionsAdapter
import com.ecc.tictactoe.view.AwaitingConnectionsAdapter
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
            host = Host(this, name)
            host.awaitingConnectionsObservable.observe(this, Observer { players ->
                awaitingConnectionsAdapter.players = players
                awaitingConnectionsAdapter.notifyDataSetChanged()
            })
            host.acceptedConnectionsObservable.observe(this, Observer { players ->
                acceptedConnectionsAdapter.players = players
                acceptedConnectionsAdapter.notifyDataSetChanged()
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

        launch_game.setOnClickListener {

            val gameFragment = GameFragment()
            gameFragment.count = 0
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.game_page, gameFragment)
                .commit()
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

