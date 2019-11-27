package com.ecc.tictactoe.connection.data

import com.google.android.gms.nearby.connection.Payload
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class PayloadData(
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String
) {
    fun toPayload(): Payload {
        return Payload.fromBytes(Gson().toJson(this).toByteArray())
    }
}

data class MetaPayloadData(
    @SerializedName("endpointId") val endpointId: String,
    @SerializedName("hostName") val hostName: String
)