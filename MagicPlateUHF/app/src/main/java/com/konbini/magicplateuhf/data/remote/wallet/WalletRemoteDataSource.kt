package com.konbini.magicplateuhf.data.remote.wallet

import android.util.Log
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import com.konbini.magicplateuhf.data.remote.wallet.request.CreditRequest
import com.konbini.magicplateuhf.data.remote.wallet.request.DebitRequest
import com.konbini.magicplateuhf.data.remote.wallet.request.WalletTokenRequest
import javax.inject.Inject

class WalletRemoteDataSource @Inject constructor(
    private val walletService: WalletService
) : BaseDataSource() {
    suspend fun getAccessToken(url: String, bodyRequest: WalletTokenRequest) = getResult {
        val path = "$url${AppSettings.APIs.Oauth}"
        Log.e("getAccessToken", path)
        walletService.getAccessToken(path, bodyRequest)
    }

    suspend fun credit(url: String, bodyRequest: CreditRequest) = getResultWithError {
        val path = "$url${AppSettings.APIs.WalletCredit}"
        walletService.credit(path, bodyRequest)
    }

    suspend fun debit(url: String, bodyRequest: DebitRequest) = getResultWithError {
        val path = "$url${AppSettings.APIs.WalletDebit}"
        walletService.debit(path, bodyRequest)
    }
}