package com.konbini.magicplateuhf.data.repository

import com.konbini.magicplateuhf.data.remote.wallet.WalletRemoteDataSource
import com.konbini.magicplateuhf.data.remote.wallet.request.CreditRequest
import com.konbini.magicplateuhf.data.remote.wallet.request.DebitRequest
import com.konbini.magicplateuhf.data.remote.wallet.request.WalletTokenRequest
import javax.inject.Inject

class WalletRepository @Inject constructor(
    private val remoteDataSource: WalletRemoteDataSource
) {
    suspend fun getAccessToken(
        url: String,
        bodyRequest: WalletTokenRequest
    ) = remoteDataSource.getAccessToken(url, bodyRequest)

    suspend fun credit(
        url: String,
        bodyRequest: CreditRequest
    ) = remoteDataSource.credit(url, bodyRequest)

    suspend fun debit(
        url: String,
        bodyRequest: DebitRequest
    ) = remoteDataSource.debit(url, bodyRequest)
}