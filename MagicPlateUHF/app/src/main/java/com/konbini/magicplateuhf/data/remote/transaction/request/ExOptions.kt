package com.konbini.magicplateuhf.data.remote.transaction.request

import com.google.gson.annotations.SerializedName

data class ExOptions(
    @SerializedName("name")
    var name: String?,
    @SerializedName("value")
    var value: String?,
    @SerializedName("type_of_price")
    var typeOfPrice: String?,
    @SerializedName("price")
    var price: Double = 0.00,
    @SerializedName("_type")
    var _type: String?
)
