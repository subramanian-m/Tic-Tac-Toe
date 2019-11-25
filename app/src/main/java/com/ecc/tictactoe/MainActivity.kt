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
import com.ecc.tictactoe.data.AlertActionConfig
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
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
    private val CODE_LENGTH = 5
    private lateinit var connectionLifecycleCallback: ConnectionLifecycleCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.connectionsClient = Nearby.getConnectionsClient(this)

        this.connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionResult(
                endpointId: String,
                connectionResolution: ConnectionResolution
            ) {
                if (connectionResolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    val payloadString = "Subramanian"
                    connectionsClient.sendPayload(
                        endpointId,
                        Payload.fromBytes(payloadString.toByteArray())
                    )
                }
            }

            override fun onDisconnected(endPointId: String) {
                Log.d("onDisconnected", "Called")
            }

            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                val title = "Accept Connection"
                val message =
                    "Do you want to accept connection request from " + info.authenticationToken
                val positiveButton = AlertActionConfig(
                    AlertDialog.BUTTON_POSITIVE,
                    "Accept",
                    DialogInterface.OnClickListener { _, _ ->
                        connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                            override fun onPayloadReceived(endPointId: String, payload: Payload) {
                                Log.d("onPayloadReceived", "Called")
                            }

                            override fun onPayloadTransferUpdate(
                                endPointId: String,
                                transferUpdate: PayloadTransferUpdate
                            ) {
                                Log.d("onPayloadTransferUpdate", "Called")
                            }
                        })
                    })
                val negativeButton = AlertActionConfig(
                    AlertDialog.BUTTON_NEGATIVE,
                    "Cancel",
                    DialogInterface.OnClickListener { _, _ ->
                        connectionsClient.rejectConnection(endpointId)
                    })
                showDialog(title, message, positiveButton, negativeButton)
            }
        }

        action_host.setOnClickListener {
            if (!isNameAvailable()) {
                return@setOnClickListener
            }

            val code = randomString(CODE_LENGTH)
            host_code.text = code
            startAdvertising(code)

            it.isEnabled = false
            action_join.isEnabled = false
            code_to_join.isEnabled = false
        }

        action_join.setOnClickListener {
            if (!isNameAvailable()) {
                return@setOnClickListener
            }

            val hostCode = code_to_join.text
            if (hostCode.toString() == "") {
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
            startDiscovery(hostCode.toString())

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

    private fun startAdvertising(code: String) {
        val advertisingTask = connectionsClient.startAdvertising(
            code,
            packageName,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        )

        advertisingTask.addOnSuccessListener {
            Log.d("AdvertisingTask", "Succeeded")
        }

        advertisingTask.addOnCanceledListener {
            Log.d("AdvertisingTask", "Cancelled")
        }

        advertisingTask.addOnCompleteListener {
            Log.d("AdvertisingTask", "Completed")
        }

        advertisingTask.addOnFailureListener {
            Log.d("AdvertisingTask", "Failed")
        }
    }

    private fun startDiscovery(code: String) {
        val discoveryTask =
            connectionsClient.startDiscovery(packageName, object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    val playerName = player_name.text.toString()
                    if (info.endpointName == code) {
                        connectionsClient.requestConnection(
                            playerName,
                            endpointId,
                            connectionLifecycleCallback
                        )
                    }
                }

                override fun onEndpointLost(endpointId: String) {
                    Log.d("onEndpointLost", endpointId)
                }
            }, DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())

        discoveryTask.addOnSuccessListener {
            Log.d("DiscoveryTask", "Succeeded")
        }

        discoveryTask.addOnCanceledListener {
            Log.d("DiscoveryTask", "Cancelled")
        }

        discoveryTask.addOnCompleteListener {
            Log.d("DiscoveryTask", "Completed")
        }

        discoveryTask.addOnFailureListener {
            Log.d("DiscoveryTask", "Failed")
        }
    }

    private fun randomString(length: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
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
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
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

