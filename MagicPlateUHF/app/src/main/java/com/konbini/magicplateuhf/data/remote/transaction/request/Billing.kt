package com.konbini.magicplateuhf.data.remote.transaction.request

import com.google.gson.annotations.SerializedName

data class Billing(
    @SerializedName("email")
    var email: String?
)
