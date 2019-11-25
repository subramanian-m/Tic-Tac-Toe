package com.ecc.tictactoe.connection

import android.content.Context
import android.util.Log
import com.ecc.tictactoe.connection.callback.PeerCallback
import com.ecc.tictactoe.data.Player
import com.ecc.tictactoe.data.removePlayer
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class Peer(private val context: Context, val name: String, val peerCallback: PeerCallback) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val acceptedConnections: MutableList<Player> = mutableListOf()

    private var hostEndpointId: String? = null
    private lateinit var host: Player
    private lateinit var selfEndpointId: String

    private val connectionLifecycleCallback: ConnectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {
            override fun onConnectionResult(
                endpointId: String,
                connectionResolution: ConnectionResolution
            ) {
                if (connectionResolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    peerCallback.connectionAccepted()
                } else {
                    peerCallback.connectionRejected()
                }
            }

            override fun onDisconnected(endpointId: String) {
                acceptedConnections.removePlayer(endpointId)
            }

            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                if (hostEndpointId != null && endpointId == hostEndpointId) {
                    host = Player(endpointId, "_host_", info.authenticationToken)
                    connectionsClient.acceptConnection(endpointId, payloadCallback)
                    peerCallback.awaitingConnection(info.authenticationToken)
                }
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
}