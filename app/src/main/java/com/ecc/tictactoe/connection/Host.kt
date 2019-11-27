package com.ecc.tictactoe.connection

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.ecc.tictactoe.connection.data.MetaPayloadData
import com.ecc.tictactoe.connection.data.PayloadData
import com.ecc.tictactoe.data.Player
import com.ecc.tictactoe.data.containsPlayer
import com.ecc.tictactoe.data.fetchPlayer
import com.ecc.tictactoe.data.removePlayer
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson

class Host(private val context: Context, val name: String) {

    private val CODE_LENGTH = 1
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val awaitingConnections: MutableList<Player> = mutableListOf()
    var acceptedConnections: MutableList<Player> = mutableListOf()
        private set

    val acceptedConnectionsObservable: MutableLiveData<List<Player>> = MutableLiveData()
    val awaitingConnectionsObservable: MutableLiveData<List<Player>> = MutableLiveData()

    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionResult(
                endpointId: String,
                connectionResolution: ConnectionResolution
            ) {
                if (connectionResolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    if (awaitingConnections.containsPlayer(endpointId)) {
                        val player = awaitingConnections.fetchPlayer(endpointId)
                        if (player != null) {
                            acceptedConnections.add(player)
                            this@Host.acceptedConnectionsObservable.value = acceptedConnections

                            awaitingConnections.removePlayer(endpointId)
                            this@Host.awaitingConnectionsObservable.value = awaitingConnections

                            val metaPayloadData = MetaPayloadData(endpointId, name)
                            val payloadData = PayloadData("meta", Gson().toJson(metaPayloadData))
                            connectionsClient.sendPayload(endpointId, payloadData.toPayload())
                        }
                    }
                }
            }

            override fun onDisconnected(endpointId: String) {
                awaitingConnections.removePlayer(endpointId)
                acceptedConnections.removePlayer(endpointId)
                this@Host.acceptedConnectionsObservable.value = acceptedConnections
                this@Host.awaitingConnectionsObservable.value = awaitingConnections
            }

            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                awaitingConnections.add(
                    Player(
                        endpointId,
                        info.endpointName,
                        info.authenticationToken
                    )
                )
                this@Host.awaitingConnectionsObservable.value = awaitingConnections
            }
        }

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(p0: String, p1: Payload) {
            Log.d("PayloadReceived", "Called")
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            Log.d("onPayloadTransferUpdate", "Called")
        }

    }

    fun advertise(): String {
        val code = randomString(CODE_LENGTH)
        val advertisingTask = connectionsClient.startAdvertising(
            code,
            context.packageName,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        )
        acceptedConnections.add(
            Player("_endpointId_", name, "_authenticationToken_")
        )
        acceptedConnectionsObservable.value = acceptedConnections

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

        return code
    }

    fun accept(endpointId: String) {
        if (awaitingConnections.containsPlayer(endpointId)) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }
    }

    fun reject(endpointId: String) {
        if (awaitingConnections.containsPlayer(endpointId)) {
            connectionsClient.rejectConnection(endpointId)
        }
    }

    fun rejectAll() {
        awaitingConnections.forEach { player ->
            reject(player.endPointId)
        }
    }

    fun stop() {
        connectionsClient.stopAdvertising()
    }

    fun sendPayload(endpointId: String, payload: String) {
        connectionsClient.sendPayload(endpointId, PayloadData("data", payload).toPayload())
    }

    private fun randomString(length: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}