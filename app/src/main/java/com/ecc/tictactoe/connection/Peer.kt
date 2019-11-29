package com.ecc.tictactoe.connection

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.ecc.tictactoe.connection.callback.PeerCallback
import com.ecc.tictactoe.connection.data.MetaPayloadData
import com.ecc.tictactoe.connection.data.PayloadData
import com.ecc.tictactoe.data.Player
import com.ecc.tictactoe.data.removePlayer
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.gson.Gson

class Peer(private val context: Context, val name: String, val peerCallback: PeerCallback) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    var acceptedConnections: MutableList<Player> = mutableListOf()
        private set

    val acceptedConnectionsObservable: MutableLiveData<List<Player>> = MutableLiveData()

    private var hostEndpointId: String? = null
    lateinit var host: Player
    lateinit var selfEndpointId: String
        private set
    private lateinit var authenticationToken: String

    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionResult(
                endpointId: String,
                connectionResolution: ConnectionResolution
            ) {
                if (connectionResolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    peerCallback.connectionAccepted()
                    connectionsClient.stopDiscovery()
                } else {
                    peerCallback.connectionRejected()
                }
            }

            override fun onDisconnected(endpointId: String) {
                acceptedConnections.removePlayer(endpointId)
                acceptedConnectionsObservable.value = acceptedConnections
            }

            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                if (hostEndpointId != null && endpointId == hostEndpointId) {
                    this@Peer.authenticationToken = info.authenticationToken
                    host = Player(endpointId, "_host_", info.authenticationToken)
                    connectionsClient.acceptConnection(endpointId, payloadCallback)
                    peerCallback.awaitingConnection(info.authenticationToken)
                }
            }
        }

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (host.endPointId == endpointId) {
                val bytes = payload.asBytes() ?: return
                val payloadData =
                    Gson().fromJson<PayloadData>(String(bytes), PayloadData::class.java)
                if (payloadData.type == "meta") {
                    val metaPayloadData = Gson().fromJson<MetaPayloadData>(
                        payloadData.value, MetaPayloadData::class.java
                    )
                    host =
                        Player(host.endPointId, metaPayloadData.hostName, host.authenticationToken)
                    selfEndpointId = metaPayloadData.endpointId

                    acceptedConnections.add(host)
                    acceptedConnections.removePlayer("_endpointId_")
                    acceptedConnections.add(Player(selfEndpointId, name, authenticationToken))
                    acceptedConnectionsObservable.value = acceptedConnections
                } else if (payloadData.type == "data") {
                    peerCallback.payloadReceived(payloadData.value)
                }
            }
        }

        override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            Log.d("onPayloadTransferUpdate", "Called")
        }

    }

    fun discover(code: String) {
        val discoveryTask =
            connectionsClient.startDiscovery(
                context.packageName,
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        if (info.endpointName == code) {
                            this@Peer.hostEndpointId = endpointId
                            connectionsClient.requestConnection(
                                name,
                                endpointId,
                                connectionLifecycleCallback
                            )
                        }
                    }

                    override fun onEndpointLost(endpointId: String) {
                        Log.d("onEndpointLost", endpointId)
                    }
                },
                DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
            )
        acceptedConnections.add(Player("_endpointId_", name, "_authenticationToken_"))
        acceptedConnectionsObservable.value = acceptedConnections

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

    fun stop() {
        connectionsClient.stopDiscovery()
    }

    fun sendPayload(endpointId: String, payload: String) {
        connectionsClient.sendPayload(endpointId, PayloadData("data", payload).toPayload())
    }
}