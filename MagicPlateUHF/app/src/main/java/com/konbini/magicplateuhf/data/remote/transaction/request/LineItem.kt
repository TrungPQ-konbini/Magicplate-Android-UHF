package com.konbini.magicplateuhf.data.remote.transaction.request

import com.google.gson.annotations.SerializedName

data class LineItem(
    @SerializedName("product_id")
    var productId: Int?,
    @SerializedName("quantity")
    var quantity: Int?,
    @SerializedName("subtotal")
    var subtotal: String?,
    @SerializedName("total")
    var total: String?,
    @SerializedName("subtotal_tax")
    var subtotalTax: String?,
    @SerializedName("total_tax")
    var totalTax: String?,
    @SerializedName("meta_data")
    var metaData: ArrayList<Any>? = arrayListOf()
)
