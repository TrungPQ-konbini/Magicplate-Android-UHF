package com.konbini.magicplateuhf.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.wallet.request.WalletTokenRequest
import com.konbini.magicplateuhf.data.repository.MenuRepository
import com.konbini.magicplateuhf.data.repository.OfflineDataRepository
import com.konbini.magicplateuhf.data.repository.TransactionRepository
import com.konbini.magicplateuhf.data.repository.WalletRepository
import com.konbini.magicplateuhf.jobs.GetTokenJobService
import com.konbini.magicplateuhf.utils.LogUtils
import com.konbini.magicplateuhf.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val walletRepository: WalletRepository,
    private val offlineDataRepository: OfflineDataRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    companion object {
        const val TAG = "SalesViewModel"
    }

    fun syncTransactions() {
        if (AppContainer.GlobalVariable.isSyncTransaction) {
            return
        }
        viewModelScope.launch {
            try {
                LogUtils.logOffline("Start Sync")
                Log.e("OFFLINE_SYNC", "Start Sync")
                AppContainer.GlobalVariable.isSyncTransaction = true
                offlineDataRepository.processOfflineData()
            } catch (ex: Exception) {
                LogUtils.logError(ex)
                AppContainer.GlobalVariable.isSyncTransaction = false
            }
        }
    }

    fun processStoreXDayLocalData() {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val startToday = calendar.timeInMillis

                val comparisonDate = Date(startToday - AppSettings.Timer.xDayStoreLocalOrders * 86400000)
                LogUtils.logInfo("[STORE_X_DAY_LOCAL_DATA] Delete transaction ≤ $comparisonDate")
                Log.e("STORE_X_DAY_LOCAL_DATA","[STORE_X_DAY_LOCAL_DATA] Delete transaction ≤ $comparisonDate")

                // Store X days of local transactions, delete the rest
                val listAllTransactions = transactionRepository.getAll()
                listAllTransactions.forEach { transactionEntity ->
                    Log.e("STORE_X_DAY_LOCAL_DATA", "${Date(transactionEntity.paymentTime.toLong())}")
                    if (comparisonDate.time > transactionEntity.paymentTime.toLong()) {
                        val logContent = "[STORE_X_DAY_LOCAL_DATA] Delete transaction date: ${Date(transactionEntity.paymentTime.toLong())} | uuid: ${transactionEntity.uuid}"
                        Log.e("STORE_X_DAY_LOCAL_DATA", logContent)
                        LogUtils.logInfo(logContent)
                        transactionRepository.deleteSingleByUuid(transactionEntity.uuid)
                    }
                }

                // Store X days of expired Menus, delete the rest
                val listAllMenu = menuRepository.getAll()
                AppContainer.GlobalVariable.listMenus.clear()
                listAllMenu.forEach { menuEntity ->
                    val menuDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(menuEntity.menuDate)
                    if (startToday - menuDate.time > AppSettings.Timer.xDayStoreLocalMenus  * 86400000) {
                        val logContent = "[STORE_X_DAY_LOCAL_DATA] Delete Menu date: ${menuEntity.menuDate} | Product: ${menuEntity.productName}"
                        Log.e("STORE_X_DAY_LOCAL_DATA", logContent)
                        LogUtils.logInfo(logContent)
                        menuRepository.deleteByMenuDate(menuEntity.menuDate)
                    } else {
                        AppContainer.GlobalVariable.listMenus.add(menuEntity)
                    }
                }
            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }

    fun getToken() {
        viewModelScope.launch {
            try {
                try {
                    LogUtils.logInfo("Call API: ${AppSettings.APIs.Oauth}")
                    Log.e(TAG, "Call API: ${AppSettings.APIs.Oauth}")
                    AppContainer.GlobalVariable.isGettingToken = true

                    // Get token wallet
                    val requestTokenWallet = WalletTokenRequest(
                        "client_credentials",
                        AppSettings.Cloud.ClientId,
                        AppSettings.Cloud.ClientSecret
                    )
                    val tokenWallet =
                        withContext(Dispatchers.Default) {
                            walletRepository.getAccessToken(
                                AppSettings.Cloud.Host,
                                requestTokenWallet
                            )
                        }

                    if (tokenWallet.status == Resource.Status.SUCCESS) {
                        tokenWallet.data?.let { _walletTokenResponse ->
                            _walletTokenResponse.access_token?.let { token ->
                                if (token.isNotEmpty()) {
                                    AppContainer.GlobalVariable.currentToken = token
                                    AppContainer.GlobalVariable.isGettingToken = false
                                    AppContainer.GlobalVariable.currentTokenLifeTimes =
                                        _walletTokenResponse.expires_in?.toLong() ?: 0L
                                }
                            }
                            LogUtils.logInfo("[Token]: ${AppContainer.GlobalVariable.currentToken}")
                            Log.e(TAG, "[Token]: ${AppContainer.GlobalVariable.currentToken}")
                        }
                    } else {
                        AppContainer.GlobalVariable.isGettingToken = false
                        LogUtils.logInfo("[Token]: Error ${Gson().toJson(tokenWallet.data ?: "Can't get Token!!!")}")
                    }
                } catch (ex: Exception) {
                    LogUtils.logError(ex)
                    AppContainer.GlobalVariable.isGettingToken = false
                }
            } catch (ex: Exception) {
                LogUtils.logError(ex)
                AppContainer.GlobalVariable.isGettingToken = false
            }
        }
    }
}