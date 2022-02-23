package com.konbini.magicplateuhf.data.remote.wallet.response

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("result")
    var result: String?,
    @SerializedName("error_code")
    var errorCode: Int?,
    @SerializedName("message")
    var message: String?
)
