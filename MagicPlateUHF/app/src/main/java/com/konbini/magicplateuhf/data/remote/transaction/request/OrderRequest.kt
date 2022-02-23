package com.konbini.magicplateuhf.data.remote.transaction.request

import com.google.gson.annotations.SerializedName

data class OrderRequest(
    @SerializedName("payment_method")
    var paymentMethod: String?,
    @SerializedName("payment_method_title")
    var paymentMethodTitle: String?,
    @SerializedName("status")
    var status: String?,
    @SerializedName("set_paid")
    var setPaid: Boolean = false,
    @SerializedName("customer_id")
    var customerId: Int?,
    @SerializedName("billing")
    var billing: Billing? = null,
    @SerializedName("meta_data")
    var metaData: ArrayList<MetaDataOrder>? = arrayListOf(),
    @SerializedName("line_items")
    var lineItems: ArrayList<LineItem>? = arrayListOf()
)