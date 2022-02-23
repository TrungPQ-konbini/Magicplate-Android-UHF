package com.konbini.magicplateuhf.data.remote.transaction.request

import com.google.gson.annotations.SerializedName

data class MetaData(
    @SerializedName("key")
    var key: String?,
    @SerializedName("value")
    var value: String?,
    @SerializedName("display_key")
    var displayKey: String?,
    @SerializedName("display_value")
    var displayValue: String?
)
