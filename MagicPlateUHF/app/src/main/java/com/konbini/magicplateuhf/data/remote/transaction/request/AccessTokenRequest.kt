package com.konbini.magicplateuhf.data.remote.transaction.request

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AccessTokenRequest(
    @Expose
    @SerializedName("grant_type")
    var grantType: String?,
    @SerializedName("client_id")
    var clientId: String?,
    @SerializedName("client_secret")
    var clientSecret: String?
)
