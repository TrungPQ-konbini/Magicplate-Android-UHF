package com.konbini.magicplateuhf.data.remote.product.response

import com.google.gson.annotations.SerializedName

data class OptionItem(
    @SerializedName("name")
    val name: String?,
    @SerializedName("price")
    val price: String?,
    @SerializedName("type")
    val type: String?,
    var isChecked: Boolean = false
)
