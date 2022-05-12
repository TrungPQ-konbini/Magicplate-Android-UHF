package com.konbini.magicplateuhf.data.remote.user.request

import com.google.gson.annotations.SerializedName

data class GetAllUserRequest(
    @SerializedName("access_token")
    var accessToken: String
)
