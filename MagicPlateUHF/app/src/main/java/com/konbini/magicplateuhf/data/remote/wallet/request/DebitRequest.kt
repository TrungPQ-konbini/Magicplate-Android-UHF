package com.konbini.magicplateuhf.data.remote.wallet.request

import com.google.gson.annotations.SerializedName

data class DebitRequest(
    @SerializedName("access_token")
    var accessToken: String?,
    @SerializedName("user_id")
    var userId: String?,
    @SerializedName("user_id_type")
    var userIdType: String?,
    @SerializedName("amount")
    var amount: Float?,
    @SerializedName("description")
    var description: String?
)
