package com.konbini.magicplateuhf.data.remote.transaction.request

import com.google.gson.annotations.SerializedName

data class SubmitTransactionRequest (
    @SerializedName("access_token")
    var accessToken: String,
    @SerializedName("mac_address")
    var macAddress: String,
    @SerializedName("txn_date_time")
    var txnDateTime: String,
    @SerializedName("txn_unique_id")
    var txnUniqueId: String,
    @SerializedName("payment_type")
    var paymentType: String,// Accepted payment_type value are "konbi_wallet" and all other non restricted value
    @SerializedName("card_number")
    var cardNumber: String,
    @SerializedName("order_status")
    var orderStatus: String,
    @SerializedName("source")
    var source: String,
    @SerializedName("terminal")
    var terminal: String,
    @SerializedName("store")
    var store: String,
    @SerializedName("others")
    var others: String,
    @SerializedName("slot_id")
    var slotId: String,
    @SerializedName("discount_type")
    var discountType: String,
    @SerializedName("ccw_id1")
    var ccwId1: String,
    @SerializedName("ccw_id2")
    var ccwId2: String,
    @SerializedName("ccw_id3")
    var ccwId3: String,
    @SerializedName("products")
    var products: ArrayList<SubmitTransactionRequestProducts> = ArrayList()
)

data class SubmitTransactionRequestProducts(
    @SerializedName("product_id")
    var productId: Int,
    @SerializedName("product_quantity")
    var productQuantity: Int,
    @SerializedName("is_custom_price")
    var isCustomPrice: Boolean,
    @SerializedName("custom_price")
    var customPrice: Double,
)