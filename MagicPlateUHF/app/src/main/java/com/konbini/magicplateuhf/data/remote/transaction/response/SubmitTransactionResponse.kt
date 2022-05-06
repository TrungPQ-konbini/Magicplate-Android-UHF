package com.konbini.magicplateuhf.data.remote.transaction.response

import com.google.gson.annotations.SerializedName

data class SubmitTransactionResponse(
    @SerializedName("result")
    var result: String,
    @SerializedName("message")
    var message: String,
    @SerializedName("detail")
    var detail: SubmitTransactionResponseDetail
)

data class SubmitTransactionResponseDetail(
    @SerializedName("order_id")
    var orderId: String,
    @SerializedName("order_status")
    var orderStatus: String,
    @SerializedName("payment_type")
    var paymentType: String,
    @SerializedName("card_number")
    var cardNumber: String,
    @SerializedName("source")
    var source: String,
    @SerializedName("terminal")
    var terminal: String,
    @SerializedName("store")
    var store: String,
    @SerializedName("products")
    var products: ArrayList<SubmitTransactionResponseProducts> = ArrayList()
)

data class SubmitTransactionResponseProducts(
    @SerializedName("product_id")
    var productId: String,
    @SerializedName("product_quantity")
    var productQuantity: String,
)