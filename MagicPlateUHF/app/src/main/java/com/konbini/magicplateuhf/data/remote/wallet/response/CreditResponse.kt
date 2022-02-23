package com.konbini.magicplateuhf.data.remote.wallet.response

import com.google.gson.annotations.SerializedName

data class CreditResponse(
    @SerializedName("transaction_id")
    var transactionId: Int?,
    @SerializedName("user_id")
    var userId: String?,
    @SerializedName("display_name")
    var displayName: String?,
    @SerializedName("amount")
    var amount: String?,
    @SerializedName("balance")
    var balance: Float?,
    @SerializedName("description")
    var description: String?,
    @SerializedName("time")
    var time: String?
)
