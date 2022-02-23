package com.konbini.magicplateuhf.data.remote.orderStatus

import com.konbini.magicplateuhf.data.remote.orderStatus.response.OrderStatusResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface OrderStatusService {
    @GET
    suspend fun syncOrderStatus(
        @Url url: String
    ): Response<OrderStatusResponse>
}