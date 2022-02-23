package com.konbini.magicplateuhf.data.remote.menu.response

import com.google.gson.annotations.SerializedName

data class MenuResponse(
    @SerializedName("results")
    val results: List<Menu> = listOf()
)

data class Menu (
    @SerializedName("date")
    val date: String?,
    @SerializedName("menus")
    val menus: List<MenuDetail> = listOf()
)

data class MenuDetail(
    @SerializedName("products")
    val products: List<Product> = listOf(),
    @SerializedName("time_block_id")
    val timeBlockId: String?
)

data class Product(
    @SerializedName("plate_model_id")
    val plateModelId: String?,
    @SerializedName("price")
    val price: String?,
    @SerializedName("product_id")
    val productId: String?
)