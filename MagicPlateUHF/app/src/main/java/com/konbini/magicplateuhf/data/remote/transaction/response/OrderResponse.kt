package com.konbini.magicplateuhf.data.remote.transaction.response

import com.google.gson.annotations.SerializedName

data class OrderResponse(
    @SerializedName("id")
    var id: Int?,
    @SerializedName("number")
    var number: String?,
    @SerializedName("order_key")
    var orderKey: String?,
    @SerializedName("created_via")
    var createdVia: String?,
    @SerializedName("status")
    var status: String?,
    @SerializedName("currency")
    var currency: String?,
    @SerializedName("date_created")
    var dateCreated: String?,
    @SerializedName("date_created_gmt")
    var dateCreatedGmt: String?,
    @SerializedName("payment_method")
    var paymentMethod: String?,
    @SerializedName("payment_method_title")
    var paymentMethodTitle: String?,
    @SerializedName("meta_data")
    var metaData: List<MetaData>? = emptyList()
)