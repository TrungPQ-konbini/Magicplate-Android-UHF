package com.konbini.magicplateuhf.data.remote.transaction.response

import com.google.gson.annotations.SerializedName

data class MetaData(
    @SerializedName("id")
    var id: Int?,
    @SerializedName("key")
    var key: String?,
    @SerializedName("value")
    var value: String?
)
