package com.konbini.magicplateuhf

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Parcelable
import android.util.Log
import com.acs.smartcard.Reader
import com.konbini.magicplateuhf.ui.SalesActivity
import com.konbini.magicplateuhf.utils.AudioManager
import com.konbini.magicplateuhf.utils.LogUtils
import com.module.interaction.ModuleConnector
import com.nativec.tools.ModuleManager
import com.rfid.RFIDReaderHelper
import com.rfid.ReaderConnector
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {

    var mainAppInit: (() -> Unit)? = null

    companion object {
        lateinit var mReaderASC: Reader
        lateinit var mManager: UsbManager
        lateinit var instance: MainApplication
        lateinit var mPermissionIntent: PendingIntent

        var isInitializedUHF = false
        lateinit var mReaderUHF: RFIDReaderHelper
        var connector: ModuleConnector = ReaderConnector()

        var currentVersion: String = "Version: N/A"

        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        fun shared(): MainApplication {
            return instance
        }
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = intent
                        .getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false
                        )
                    ) {
                        if (device != null) {
                            try {
                                if (device.manufacturerName == "ACS")
                                    mReaderASC.open(device)
                            } catch (ex: Exception) {
                                // Close reader
                                mReaderASC.close()
                            }
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                synchronized(this) {
                    val device = intent
                        .getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (device != null && device == mReaderASC.device) {
                        // Close reader
                        mReaderASC.close()
                    }
                }
            }
        }
    }

    init {
        this.also { instance = it }
    }

    override fun onCreate() {
        LogUtils.logInfo("Start App")
        initSetting()
        mainAppInit?.invoke()
        instance = this
        AudioManager(this)
        getAppVersion()
        initAcsReader()
        initRFIDReaderUHF()
        super.onCreate()
    }

    private fun initSetting() {
        AppSettings.getAllSetting()
    }

    /**
     * Get current version.
     */
    private fun getAppVersion() {
        try {
            val pInfo = this.packageManager.getPackageInfo(this.packageName, 0)
            currentVersion = "Version: " + pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            return
        }
    }

    /**
     * Init Acs Reader
     */
    private fun initAcsReader() {
        LogUtils.logInfo("Start Acs Reader Service")

        // Get USB manager
        mManager = getSystemService(USB_SERVICE) as UsbManager

        // Initialize reader
        mReaderASC = Reader(mManager)

        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(
                ACTION_USB_PERMISSION
            ), 0
        )
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(mReceiver, filter)

        // For each device
        for (device in mManager.deviceList.values) {

            // If device name is found
            if ("ACS" == device.manufacturerName) {

                // Request permission
                mManager.requestPermission(
                    device,
                    mPermissionIntent
                )
            }
        }
    }

    /**
     * Init RFID Reader UHF
     *
     */
    private fun initRFIDReaderUHF() {
        try {
            if (connector.connectCom(
                    AppSettings.Machine.ReaderUHF,
                    AppSettings.Machine.ReaderUHFBaudRate
                )
            ) {
                ModuleManager.newInstance().uhfStatus = true
                try {
                    mReaderUHF = RFIDReaderHelper.getDefaultHelper()
                    isInitializedUHF = true
                } catch (ex: Exception) {
                    Log.e(SalesActivity.TAG, ex.toString())
                    LogUtils.logError(ex)
                }
            }
        } catch (ex: Exception) {
            Log.e(SalesActivity.TAG, ex.toString())
            LogUtils.logError(ex)
        }
    }
}