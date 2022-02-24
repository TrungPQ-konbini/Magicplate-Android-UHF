package com.konbini.magicplateuhf.base

import com.google.gson.annotations.SerializedName

data class MessageMQTT(
    @SerializedName("menu")
    val menu: Boolean,
    @SerializedName("plate_model")
    val plateModel: Boolean,
    @SerializedName("time_block")
    val timeBlock: Boolean,
    @SerializedName("product")
    val product: Boolean,
)
