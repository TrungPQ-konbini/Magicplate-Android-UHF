package com.konbini.magicplateuhf.data.remote.transaction.request

import com.google.gson.annotations.SerializedName

data class MetaDataOrder(
    @SerializedName("key")
    var key: String? = "",
    @SerializedName("value")
    var value: String? = ""
)
