package com.konbini.magicplateuhf.data.remote.product.response

import com.google.gson.annotations.SerializedName

data class MetaData(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("key")
    val key: String?,
    @SerializedName("value")
    val value: Any?
)
