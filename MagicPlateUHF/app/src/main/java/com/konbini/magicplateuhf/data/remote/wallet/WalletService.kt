package com.konbini.magicplateuhf.data.remote.wallet

import com.konbini.magicplateuhf.data.remote.wallet.request.CreditRequest
import com.konbini.magicplateuhf.data.remote.wallet.request.DebitRequest
import com.konbini.magicplateuhf.data.remote.wallet.request.WalletTokenRequest
import com.konbini.magicplateuhf.data.remote.wallet.response.CreditResponse
import com.konbini.magicplateuhf.data.remote.wallet.response.DebitResponse
import com.konbini.magicplateuhf.data.remote.wallet.response.WalletTokenResponse
import retrofit2.Response
import retrofit2.http.*

interface WalletService {
    @POST
    suspend fun getAccessToken(
        @Url url: String,
        @Body bodyRequest: WalletTokenRequest
    ): Response<WalletTokenResponse>

    @POST
    suspend fun credit(
        @Url url: String,
        @Body bodyRequest: CreditRequest
    ): Response<CreditResponse>

    @POST
    suspend fun debit(
        @Url url: String,
        @Body bodyRequest: DebitRequest
    ): Response<DebitResponse>
}