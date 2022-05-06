package com.konbini.magicplateuhf.data.remote.transaction

import com.konbini.magicplateuhf.data.remote.transaction.request.OrderRequest
import com.konbini.magicplateuhf.data.remote.transaction.request.SubmitTransactionRequest
import com.konbini.magicplateuhf.data.remote.transaction.response.OrderResponse
import com.konbini.magicplateuhf.data.remote.transaction.response.SubmitTransactionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface TransactionService {
    @POST
    suspend fun createAnOrder(
        @Url url: String,
        @Body bodyRequest: OrderRequest,
        @Query("consumer_key") consumerKey: String?,
        @Query("consumer_secret") consumerSecret: String?
    ): Response<OrderResponse>

    @POST
    suspend fun submitTransaction(
        @Url url: String,
        @Body bodyRequest: SubmitTransactionRequest
    ): Response<SubmitTransactionResponse>
}