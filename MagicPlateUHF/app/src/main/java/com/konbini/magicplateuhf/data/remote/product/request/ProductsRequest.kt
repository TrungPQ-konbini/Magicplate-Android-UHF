package com.konbini.magicplateuhf.data.remote.product.request

import com.google.gson.annotations.SerializedName

data class ProductsRequest(
    @SerializedName("page")
    val page: Int?,
    @SerializedName("per_page")
    val perPage: Int?,
    @SerializedName("consumer_key")
    val consumerKey: String?,
    @SerializedName("consumer_secret")
    val consumerSecret: String?
)
