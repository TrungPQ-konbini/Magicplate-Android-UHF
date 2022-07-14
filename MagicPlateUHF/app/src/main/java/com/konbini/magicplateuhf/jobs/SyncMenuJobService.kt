package com.konbini.magicplateuhf.jobs

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.entities.PlateModelEntity
import com.konbini.magicplateuhf.data.entities.ProductEntity
import com.konbini.magicplateuhf.data.repository.*
import com.konbini.magicplateuhf.ui.settings.SettingsViewModel
import com.konbini.magicplateuhf.utils.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class SyncMenuJobService : JobService() {

    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var productRepository: ProductRepository
    @Inject lateinit var plateModelRepository: PlateModelRepository
    @Inject lateinit var timeBlockRepository: TimeBlockRepository
    @Inject lateinit var menuRepository: MenuRepository
    @Inject lateinit var orderStatusRepository: OrderStatusRepository
    @Inject lateinit var userRepository: UserRepository

    private val serviceScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    companion object {
        private const val TAG = "SyncMenuJobService"

        lateinit var settingsViewModel: SettingsViewModel
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Job Sync Menu Started")
        LogUtils.logInfo("Job Sync Menu Started")
        settingsViewModel = SettingsViewModel(
            categoryRepository,
            productRepository,
            plateModelRepository,
            timeBlockRepository,
            menuRepository,
            orderStatusRepository,
            userRepository
        )
        doBackgroundWork(params)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Job Sync Menu completion")
        return true
    }

    private fun doBackgroundWork(params: JobParameters?) {
        Thread(Runnable {
            kotlin.run {
                if (AppSettings.Options.AllowAutoSyncMenu) {
                    serviceScope.launch {
                        try {
                            LogUtils.logInfo("Start Auto Sync Menu")
                            Log.e(TAG, "Start Sync Menu")

                            settingsViewModel.syncAll()

                        } catch (ex: Exception) {
                            LogUtils.logError(ex)
                        }
                    }
                }
                Thread.sleep(AppSettings.Timer.PeriodicAutoSyncMenu.toLong() * 60 * 1000)
            }
            jobFinished(params, true)
        }).start()
    }
}