package com.konbini.magicplateuhf.data.remote.orderStatus.response

data class OrderStatusResponse(
    val result: String,
    val message: String,
    val data: ArrayList<String>
)
