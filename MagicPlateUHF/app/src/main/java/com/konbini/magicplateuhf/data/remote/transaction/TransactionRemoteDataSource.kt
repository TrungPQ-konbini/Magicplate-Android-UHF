package com.konbini.magicplateuhf.data.remote.transaction

import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import com.konbini.magicplateuhf.data.remote.transaction.request.OrderRequest
import com.konbini.magicplateuhf.data.remote.transaction.request.SubmitTransactionRequest
import javax.inject.Inject

class TransactionRemoteDataSource @Inject constructor(
    private val transactionService: TransactionService
) : BaseDataSource() {
    suspend fun createAnOrder(url: String, bodyRequest: OrderRequest)  = getResult {
        val path = "$url${AppSettings.APIs.CreateAnOrder}"
        val consumerKey = AppSettings.Cloud.ConsumerKey
        val consumerSecret = AppSettings.Cloud.ConsumerSecret
        transactionService.createAnOrder(path, bodyRequest, consumerKey, consumerSecret)
    }

    suspend fun submitTransaction(url: String, bodyRequest: SubmitTransactionRequest) = getResult {
        val path = "$url${AppSettings.APIs.SubmitTransaction}"
        transactionService.submitTransaction(path, bodyRequest)
    }
}