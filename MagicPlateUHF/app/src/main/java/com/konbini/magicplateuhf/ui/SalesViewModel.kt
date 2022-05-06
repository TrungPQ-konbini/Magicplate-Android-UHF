package com.konbini.magicplateuhf.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.repository.MenuRepository
import com.konbini.magicplateuhf.data.repository.OfflineDataRepository
import com.konbini.magicplateuhf.data.repository.TransactionRepository
import com.konbini.magicplateuhf.utils.LogUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val offlineDataRepository: OfflineDataRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
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
                    Log.e("STORE_X_DAY_LOCAL_DATA", "${Date(transactionEntity.paymentTime)}")
                    if (comparisonDate.time > transactionEntity.paymentTime.toLong()) {
                        val logContent = "[STORE_X_DAY_LOCAL_DATA] Delete transaction date: ${Date(transactionEntity.paymentTime)} | uuid: ${transactionEntity.uuid}"
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
}