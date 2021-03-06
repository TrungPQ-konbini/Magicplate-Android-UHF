package com.konbini.magicplateuhf.ui

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.databinding.ActivitySalesBinding
import com.konbini.magicplateuhf.jobs.GetTokenJobService
import com.konbini.magicplateuhf.ui.plateModel.PlateModelViewModel
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.getDateJob
import com.konbini.magicplateuhf.utils.LogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SalesActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SalesActivity"
        private var defaultInterval: Int = 1000
        private var lastTimeClicked: Long = 0

        const val SUCCESS_KEY = "SUCCESS"
        const val FAILED_KEY = "FAILED"
        const val JOB_GET_TOKEN_ID = 123
    }

    private var barcode = ""
    private lateinit var jobTimerTask: TimerTask
    private lateinit var jobXDayTimerTask: TimerTask

    private lateinit var binding: ActivitySalesBinding
    private val viewModel: SalesViewModel by viewModels()
    private val viewModelPlateModel: PlateModelViewModel by viewModels()

    private val timer = object : CountDownTimer(
        AppSettings.Timer.PeriodicSyncOffline.toLong() * 60 * 1000,
        1000
    ) {
        override fun onTick(millisUntilFinished: Long) {}

        override fun onFinish() {
            syncTransactions()
            start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get all plate model
        lifecycleScope.launch {
            AppContainer.GlobalVariable.listPlatesModel =
                viewModelPlateModel.getAll().toMutableList()
        }
    }

    override fun onResume() {
        AppContainer.GlobalVariable.isBackend = false
        if (!AppSettings.Options.Sync.NoSyncOrder) {
            if (AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod || AppSettings.Options.Sync.SyncOrderRealtime) {
                syncTransactions()
                timer.cancel()
                timer.start()
            }

            if (AppSettings.Options.Sync.SyncOrderSpecifiedTime) {
                timer.cancel()
                jobSyncOrderSpecifiedTime()
            }
        }
        jobStoreXDayLocalData()
        scheduleGetTokenJob()
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN) {
            val pressedKey = e.unicodeChar.toChar()
            if (e.keyCode != 115)
                barcode += pressedKey
        }
        Log.e("KEY_CODE", e.keyCode.toString())
        if (e.action == KeyEvent.ACTION_DOWN) {
            when (e.keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
                        barcode = ""
                        return super.dispatchKeyEvent(e)
                    }
                    lastTimeClicked = SystemClock.elapsedRealtime()
                    AppContainer.CurrentTransaction.barcode = barcode
                    Log.e("BARCODE_VALUE", AppContainer.CurrentTransaction.barcode)
                    val intent = Intent()
                    intent.action = "NEW_BARCODE"
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                    barcode = ""
                }
                KeyEvent.KEYCODE_NUM_LOCK,
                KeyEvent.KEYCODE_NUMPAD_0,
                KeyEvent.KEYCODE_NUMPAD_1,
                KeyEvent.KEYCODE_NUMPAD_2,
                KeyEvent.KEYCODE_NUMPAD_3,
                KeyEvent.KEYCODE_NUMPAD_4,
                KeyEvent.KEYCODE_NUMPAD_5,
                KeyEvent.KEYCODE_NUMPAD_6,
                KeyEvent.KEYCODE_NUMPAD_7,
                KeyEvent.KEYCODE_NUMPAD_8,
                KeyEvent.KEYCODE_NUMPAD_9,
                KeyEvent.KEYCODE_NUMPAD_DIVIDE,
                KeyEvent.KEYCODE_NUMPAD_MULTIPLY,
                KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
                KeyEvent.KEYCODE_NUMPAD_ADD,
                KeyEvent.KEYCODE_NUMPAD_DOT,
                KeyEvent.KEYCODE_NUMPAD_COMMA,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_NUMPAD_EQUALS -> {
                    var pressedKey = e.displayLabel.toString()
                    when (e.keyCode) {
                        KeyEvent.KEYCODE_NUM_LOCK -> {
                            pressedKey = "KEYCODE_NUM_LOCK"
                        }
                        KeyEvent.KEYCODE_NUMPAD_0 -> {
                            pressedKey = "KEYCODE_NUMPAD_0"
                        }
                        KeyEvent.KEYCODE_NUMPAD_1 -> {
                            pressedKey = "KEYCODE_NUMPAD_1"
                        }
                        KeyEvent.KEYCODE_NUMPAD_2 -> {
                            pressedKey = "KEYCODE_NUMPAD_2"
                        }
                        KeyEvent.KEYCODE_NUMPAD_3 -> {
                            pressedKey = "KEYCODE_NUMPAD_3"
                        }
                        KeyEvent.KEYCODE_NUMPAD_4 -> {
                            pressedKey = "KEYCODE_NUMPAD_4"
                        }
                        KeyEvent.KEYCODE_NUMPAD_5 -> {
                            pressedKey = "KEYCODE_NUMPAD_5"
                        }
                        KeyEvent.KEYCODE_NUMPAD_6 -> {
                            pressedKey = "KEYCODE_NUMPAD_6"
                        }
                        KeyEvent.KEYCODE_NUMPAD_7 -> {
                            pressedKey = "KEYCODE_NUMPAD_7"
                        }
                        KeyEvent.KEYCODE_NUMPAD_8 -> {
                            pressedKey = "KEYCODE_NUMPAD_8"
                        }
                        KeyEvent.KEYCODE_NUMPAD_9 -> {
                            pressedKey = "KEYCODE_NUMPAD_9"
                        }
                        KeyEvent.KEYCODE_NUMPAD_DIVIDE -> {
                            pressedKey = "KEYCODE_NUMPAD_DIVIDE"
                        }
                        KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> {
                            pressedKey = "KEYCODE_NUMPAD_MULTIPLY"
                        }
                        KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> {
                            pressedKey = "KEYCODE_NUMPAD_SUBTRACT"
                        }
                        KeyEvent.KEYCODE_NUMPAD_ADD -> {
                            pressedKey = "KEYCODE_NUMPAD_ADD"
                        }
                        KeyEvent.KEYCODE_NUMPAD_DOT -> {
                            pressedKey = "KEYCODE_NUMPAD_DOT"
                        }
                        KeyEvent.KEYCODE_NUMPAD_COMMA -> {
                            pressedKey = "KEYCODE_NUMPAD_COMMA"
                        }
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            pressedKey = "KEYCODE_NUMPAD_ENTER"
                        }
                        KeyEvent.KEYCODE_NUMPAD_EQUALS -> {
                            pressedKey = "KEYCODE_NUMPAD_EQUALS"
                        }
                    }
                    if (pressedKey != "??????") {
                        val intent = Intent()
                        intent.action = "KEY_CODE"
                        intent.putExtra("pressedKey", pressedKey)
                        LocalBroadcastManager.getInstance(MainApplication.instance.applicationContext).sendBroadcast(intent)
                    }
                }
            }
        }
        return super.dispatchKeyEvent(e)
    }

    private fun syncTransactions() {
        viewModel.syncTransactions()
    }

    private fun jobStoreXDayLocalData(isNextDay: Boolean = false) {
        if (!isNextDay) {
            viewModel.processStoreXDayLocalData()
        }
        val dateJob = getDateJob(isNextDay, 1, 1)

        jobXDayTimerTask = object : TimerTask() {
            override fun run() {
                viewModel.processStoreXDayLocalData()
                jobStoreXDayLocalData(isNextDay = true)
            }

        }

        Log.e(TAG, "Next schedule is $dateJob")
        LogUtils.logOffline("[STORE_X_DAY_LOCAL_DATA] Next schedule is $dateJob")
        Timer().schedule(jobXDayTimerTask, dateJob)
    }

    private fun jobSyncOrderSpecifiedTime(isNextDay: Boolean = false) {
        val dateJob = getDateJob(
            isNextDay,
            AppSettings.Timer.SpecifiedTimeHour,
            AppSettings.Timer.SpecifiedTimeMinute
        )

        jobTimerTask = object : TimerTask() {
            override fun run() {
                LogUtils.logOffline("Start Sync at $dateJob")
                syncTransactions()
                jobSyncOrderSpecifiedTime(isNextDay = true)
            }

        }

        Log.e("OFFLINE_SYNC", "Next schedule is $dateJob")
        LogUtils.logOffline("Next schedule is $dateJob")
        Timer().schedule(jobTimerTask, dateJob)
    }

    private fun scheduleGetTokenJob() {
        val periodicGetToken: Long = AppSettings.Timer.PeriodicGetToken.toLong() * 60 * 1000
        val componentName = ComponentName(this, GetTokenJobService::class.java)
        val info = JobInfo.Builder(JOB_GET_TOKEN_ID, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setPersisted(true)
            .setPeriodic(periodicGetToken)
            .build()

        val jobScheduler: JobScheduler =
            getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val resultCode = jobScheduler.schedule(info)

        val isJobScheduledSuccess = resultCode == JobScheduler.RESULT_SUCCESS
        LogUtils.logInfo("Job Scheduled Get Token ${if (isJobScheduledSuccess) SUCCESS_KEY else FAILED_KEY}")
    }
}