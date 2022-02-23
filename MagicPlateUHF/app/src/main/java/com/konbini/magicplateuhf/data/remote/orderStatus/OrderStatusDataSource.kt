package com.konbini.magicplateuhf.data.remote.orderStatus

import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import javax.inject.Inject

class OrderStatusDataSource@Inject constructor(
    private val orderStatusService: OrderStatusService
): BaseDataSource() {
    suspend fun syncOrderStatus(
        url: String
    ) = getResult {
        val api = AppSettings.APIs.ListAllOrderStatus
        val path = "$url$api"
        orderStatusService.syncOrderStatus(path)
    }
}