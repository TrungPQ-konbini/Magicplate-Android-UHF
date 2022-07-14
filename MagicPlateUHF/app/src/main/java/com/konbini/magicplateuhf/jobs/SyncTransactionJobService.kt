package com.konbini.magicplateuhf.jobs

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.repository.OfflineDataRepository
import com.konbini.magicplateuhf.utils.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class SyncTransactionJobService : JobService() {

    @Inject lateinit var offlineDataRepository: OfflineDataRepository
    private val serviceScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    companion object {
        private const val TAG = "SyncTransactionJobService"
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Job Sync Transaction Started")
        LogUtils.logInfo("Job Sync Transaction Started")
        doBackgroundWork(params)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Job Sync Transaction completion")
        return true
    }

    private fun doBackgroundWork(params: JobParameters?) {
        Thread(Runnable {
            kotlin.run {
                if (!AppContainer.GlobalVariable.isSyncTransaction) {
                    serviceScope.launch {
                        try {
                            LogUtils.logInfo("Start Sync Transactions")
                            Log.e(TAG, "Start Sync Transactions")
                            AppContainer.GlobalVariable.isSyncTransaction = true
                            offlineDataRepository.processOfflineData()
                        } catch (ex: Exception) {
                            LogUtils.logError(ex)
                            AppContainer.GlobalVariable.isSyncTransaction = false
                        }
                    }
                }
                Thread.sleep(15 * 60 * 1000)
            }
            jobFinished(params, true)
        }).start()
    }
}