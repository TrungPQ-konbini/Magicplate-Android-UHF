package com.konbini.magicplateuhf.data.remote.transaction.request

import com.google.gson.annotations.SerializedName

data class MetaDataOption(
    @SerializedName("key")
    var key: String? = "_exoptions",
    @SerializedName("value")
    var value: ArrayList<ExOptions>? = arrayListOf()
)
