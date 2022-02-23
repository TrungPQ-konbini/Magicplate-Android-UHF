package com.konbini.magicplateuhf.data.repository

import com.konbini.magicplateuhf.data.remote.orderStatus.OrderStatusDataSource
import javax.inject.Inject

class OrderStatusRepository @Inject constructor(
    private val remoteDataSource: OrderStatusDataSource
) {
    suspend fun syncOrderStatus(url: String) =
        remoteDataSource.syncOrderStatus(url)

}