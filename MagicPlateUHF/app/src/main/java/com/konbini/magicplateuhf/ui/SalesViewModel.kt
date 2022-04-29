package com.konbini.magicplateuhf.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.wallet.request.WalletTokenRequest
import com.konbini.magicplateuhf.data.repository.WalletRepository
import com.konbini.magicplateuhf.utils.LogUtils
import com.konbini.magicplateuhf.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {
    fun getToken() {
        viewModelScope.launch {
            try {
                // Get token wallet
                val requestTokenWallet = WalletTokenRequest(
                    "client_credentials",
                    AppSettings.Cloud.ClientId,
                    AppSettings.Cloud.ClientSecret
                )
                val tokenWallet = async {
                    walletRepository.getAccessToken(AppSettings.Cloud.Host, requestTokenWallet)
                }

                if (tokenWallet.await().status == Resource.Status.SUCCESS) {
                    tokenWallet.await().data?.let { _walletTokenResponse ->
                        if (_walletTokenResponse.access_token != null) {
                            AppContainer.GlobalVariable.currentToken =
                                _walletTokenResponse.access_token!!
                        }
                    }
                }
            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }
}