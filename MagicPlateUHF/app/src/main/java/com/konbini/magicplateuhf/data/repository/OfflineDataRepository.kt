package com.konbini.magicplateuhf.data.repository

import android.util.Log
import com.google.gson.Gson
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.entities.OfflineDataEntity
import com.konbini.magicplateuhf.data.entities.TransactionEntity
import com.konbini.magicplateuhf.data.enum.OfflineDataType
import com.konbini.magicplateuhf.data.local.offlineData.OfflineDataDao
import com.konbini.magicplateuhf.data.local.transaction.TransactionDao
import com.konbini.magicplateuhf.data.remote.transaction.TransactionRemoteDataSource
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.formatCreateAnOrderRequest
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.formatSubmitTransactionRequest
import com.konbini.magicplateuhf.utils.LogUtils
import com.konbini.magicplateuhf.utils.Resource
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class OfflineDataRepository @Inject constructor(
    private val localOfflineDataSource: OfflineDataDao,
    private val localTransactionDataSource: TransactionDao,
    private val transactionRemoteDataSource: TransactionRemoteDataSource
) {
    companion object {
        private const val TAG = "OfflineDataRepository"
    }

    fun insert(offlineData: Any) {
        val calendar = Calendar.getInstance()
        val syncedDate = calendar.timeInMillis
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (offlineData) {
                    is TransactionEntity -> {
                        val json = Gson().toJson(offlineData)
                        val type = OfflineDataType.CreateAnOrderApi.value
                        LogUtils.logOffline("Save offline data: type $type | json: $json")
                        localOfflineDataSource.insert(
                            OfflineDataEntity(
                                id = 0,
                                uuid = UUID.randomUUID().toString(),
                                dataType = type,
                                json = json,
                                createdDate = syncedDate.toString(),
                                isSynced = false
                            )
                        )
                    }
                    else -> {
                        val json = Gson().toJson(offlineData)
                        LogUtils.logOffline("Unexpected Offline data type | json: $json")
                    }
                }
            } catch (e: Exception) {
                LogUtils.logError(e)
            }
        }
    }

    suspend fun processOfflineData() {
        // Get all not synced data
        val data = localOfflineDataSource.getNotSyncedData()
        LogUtils.logOffline("Data not sync | ${Gson().toJson(data)}")
        if (data.isEmpty()) {
            LogUtils.logOffline("All offline data has been synced")
        } else {
            LogUtils.logOffline("Found ${data.count()} offline data | Processing to sync again")

            withContext(Dispatchers.IO) {
                data.forEachIndexed { index, item ->
                    when (val type = enumValueOf<OfflineDataType>(item.dataType)) {
                        OfflineDataType.CreateAnOrderApi -> {
                            val syncObj = Gson().fromJson(
                                item.json,
                                TransactionEntity::class.java
                            )
                            if (syncObj.details.isNullOrEmpty()) return@forEachIndexed
                            if (AppSettings.APIs.UseNativeWoo) {
                                val bodyRequest = formatCreateAnOrderRequest(syncObj)

                                val createAnOrder =
                                    withContext(Dispatchers.Default) {
                                        transactionRemoteDataSource.createAnOrder(
                                            url = AppSettings.Cloud.Host,
                                            bodyRequest
                                        )
                                    }
                                LogUtils.logOffline("- Syncing $type data | Result:${createAnOrder.data}")
                                if (createAnOrder.status == Resource.Status.SUCCESS) {
                                    createAnOrder.data?.let { _createAnOrder ->
                                        item.isSynced = true
                                        // localOfflineDataSource.update(item)
                                        localOfflineDataSource.update(item.uuid, item.isSynced)

                                        // Update database order number
                                        val syncId = _createAnOrder.number?.toInt()
                                        val calendar = Calendar.getInstance()
                                        val syncedDate = calendar.timeInMillis.toString()
                                        localTransactionDataSource.update(
                                            syncObj.uuid,
                                            syncId,
                                            syncedDate
                                        )
                                    }
                                }
                            } else {
                                // use submit transaction API's Daniel
                                val bodyRequest = formatSubmitTransactionRequest(syncObj)

                                Log.e("OFFLINE_SYNC", "$index. ${syncObj.uuid}")
                                LogUtils.logOffline("=============================================")
                                LogUtils.logOffline("- Sync UUID: $index. ${syncObj.uuid}")
                                LogUtils.logOffline(
                                    "- Syncing $type data | Request:${
                                        Gson().toJson(
                                            bodyRequest
                                        )
                                    }"
                                )
                                val submitTransaction = async {
                                    transactionRemoteDataSource.submitTransaction(
                                        url = AppSettings.Cloud.Host,
                                        bodyRequest
                                    )
                                }

                                LogUtils.logOffline("- Syncing $type data | Result:${submitTransaction.await().data}")
                                if (submitTransaction.await().status == Resource.Status.SUCCESS) {
                                    submitTransaction.await().data?.let { _submitTransaction ->
                                        item.isSynced = true
                                        // localOfflineDataSource.update(item)
                                        localOfflineDataSource.update(item.uuid, item.isSynced)

                                        // Update database order number
                                        val syncId = _submitTransaction.detail.orderId.toLong()
                                        val calendar = Calendar.getInstance()
                                        val syncedDate = calendar.timeInMillis
                                        localTransactionDataSource.update(
                                            syncObj.uuid,
                                            syncId.toInt(),
                                            syncedDate.toString()
                                        )
                                        LogUtils.logOffline("- Synced UUID:${syncObj.uuid}")
                                    }
                                }
                            }
                        }
                        //==========================================================================
                    }
                    delay(5000)
                }
                LogUtils.logOffline("Finish Sync")
                Log.e(TAG,"Finish Sync")
                AppContainer.GlobalVariable.isSyncTransaction = false
            }
        }
    }
}