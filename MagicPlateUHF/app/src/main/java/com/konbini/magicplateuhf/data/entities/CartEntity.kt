package com.konbini.magicplateuhf.data.entities

data class CartEntity(
    var uuid: String = "",
    var strEPC: String = "",
    val menuDate: String = "",
    val timeBlockId: String = "",
    val productId: String = "",
    val plateModelId: String = "",
    val price: String = "",
    var productName: String = "",
    val plateModelName: String = "",
    val plateModelCode: String = "",
    val timeBlockTitle: String = "",
    var quantity: Int = 1,
    var options: String = ""
)
