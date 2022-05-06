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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.acs.smartcard.Reader
import com.google.gson.Gson
import com.konbini.im30.hardware.IM30Interface
import com.konbini.magicplateuhf.base.MessageMQTT
import com.konbini.magicplateuhf.ui.SalesActivity
import com.konbini.magicplateuhf.utils.AudioManager
import com.konbini.magicplateuhf.utils.LogUtils
import com.konbini.magicplateuhf.utils.MqttHelper
import com.module.interaction.ModuleConnector
import com.nativec.tools.ModuleManager
import com.rfid.RFIDReaderHelper
import com.rfid.ReaderConnector
import com.rfid.rxobserver.RXObserver
import com.rfid.rxobserver.bean.RXInventoryTag
import dagger.hilt.android.HiltAndroidApp
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

@HiltAndroidApp
class MainApplication : Application() {

    var mainAppInit: (() -> Unit)? = null

    lateinit var mqttHelper: MqttHelper

    companion object {
        const val TAG = "MainApplication"
        lateinit var mReaderASC: Reader
        lateinit var mManager: UsbManager
        lateinit var instance: MainApplication
        lateinit var mPermissionIntent: PendingIntent

        lateinit var mReaderUHF: RFIDReaderHelper
        var connector: ModuleConnector = ReaderConnector()

        var currentVersion: String = "Version: N/A"
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        var tagSizeOld = 0
        var timeTagSizeChanged = 0L

        private var rxObserver: RXObserver = object : RXObserver() {
            override fun onInventoryTag(tag: RXInventoryTag) {
                Log.e(TAG, tag.strEPC)
                AppContainer.GlobalVariable.listEPC.add(tag.strEPC.replace("\\s".toRegex(), ""))
            }

            override fun onInventoryTagEnd(endTag: RXInventoryTag.RXInventoryTagEnd) {
                Log.e(
                    TAG,
                    "==========End command reading UHF=========="
                )

                val current = System.currentTimeMillis()
                if (AppContainer.CurrentTransaction.listEPC.size != AppContainer.GlobalVariable.listEPC.size) {
                    if (timeTagSizeChanged == 0L) {
                        timeTagSizeChanged = current
                    } else {
                        val offset = current - timeTagSizeChanged
                        if (offset < 500) {
                            Log.e(TAG, "$current | $offset => Ignore")
                        } else {
                            Log.e(TAG, "listEPC: ${AppContainer.GlobalVariable.listEPC.size} | tagSizeOld: ${AppContainer.CurrentTransaction.listEPC.size}")
                            AppContainer.CurrentTransaction.listEPC.clear()
                            AppContainer.CurrentTransaction.listEPC.addAll(AppContainer.GlobalVariable.listEPC)

                            // Get list tags
                            val listTagEntity = AppContainer.GlobalVariable.getListTagEntity(AppContainer.GlobalVariable.listEPC)
                            AppContainer.CurrentTransaction.listTagEntity = listTagEntity

                            timeTagSizeChanged = 0L
                            AppContainer.CurrentTransaction.refreshCart()

                            // Add or Remove items to cart
                            val intent = Intent()
                            intent.action = "REFRESH_TAGS"
                            LocalBroadcastManager.getInstance(instance.applicationContext).sendBroadcast(intent)
                        }
                    }
                } else {
                    AppContainer.CurrentTransaction.listEPC.clear()
                    AppContainer.CurrentTransaction.listEPC.addAll(AppContainer.GlobalVariable.listEPC)

                    // Get list tags
                    val listTagEntity = AppContainer.GlobalVariable.getListTagEntity(AppContainer.GlobalVariable.listEPC)
                    AppContainer.CurrentTransaction.listTagEntity = listTagEntity

                    timeTagSizeChanged = 0L
                    AppContainer.CurrentTransaction.refreshCart()

                    // Add or Remove items to cart
                    val intent = Intent()
                    intent.action = "REFRESH_TAGS"
                    LocalBroadcastManager.getInstance(instance.applicationContext).sendBroadcast(intent)
                }

                if (AppContainer.GlobalVariable.allowReadTags) {
                    // Start reading UHF
                    mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
                    Log.e(
                        TAG,
                        "==========Start command reading UHF=========="
                    )
                }

                AppContainer.GlobalVariable.listEPC.clear()
            }
        }

        fun shared(): MainApplication {
            return instance
        }

        fun startRealTimeInventory() {
            AppContainer.GlobalVariable.allowReadTags = true
            mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
        }

        fun initIM30() {
            IM30Interface(instance)
            val ports = IM30Interface.instance.port.getDevicesList().toList()
            IM30Interface.instance.setLogger { log ->
                Log.d("IM30", log)
                LogUtils.logInfo(log)
            }

            ports.forEach { _port ->
                LogUtils.logInfo(_port.second.productName.toString())
                if (_port.second.productName == "USB Serial Converter"
                    || _port.second.productName == "Serrial device - FTDI"
                    || _port.second.productName == "FTDI"
                ) {
                    LogUtils.logInfo("payment _ name _ ${_port.second.productName}")
                    if (IM30Interface.instance.isInit) {
                        LogUtils.logInfo("IUC opened _ ${_port.second.productName}")
                        IM30Interface.instance.open(_port.second.deviceName)
                    }
                }
            }
        }

        /**
         * Init RFID Reader UHF
         *
         */
        fun initRFIDReaderUHF() {
            try {
                if (connector.connectCom(
                        AppSettings.Hardware.Comport.ReaderUHF,
                        AppSettings.Hardware.Comport.ReaderUHFBaudRate
                    )
                ) {
                    ModuleManager.newInstance().uhfStatus = true
                    try {
                        mReaderUHF = RFIDReaderHelper.getDefaultHelper()
                        mReaderUHF.unRegisterObserver(rxObserver)
                        mReaderUHF.registerObserver(rxObserver)
                        Thread.sleep(500)
                        startRealTimeInventory()
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
        initMQTT()
        initAcsReader()
        initRFIDReaderUHF()
        initIM30()
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
     * Init MQTT
     *
     */
    private fun initMQTT() {
        if (this::mqttHelper.isInitialized) return

        val host = AppSettings.MQTT.Host
        val userName = AppSettings.MQTT.UserName
        val password = AppSettings.MQTT.Password
        val topic = AppSettings.MQTT.Topic

        if (host.isNullOrEmpty() || userName.isNullOrEmpty() || password.isNullOrEmpty() || topic.isNullOrEmpty()) return

        mqttHelper = MqttHelper(
            applicationContext,
            host,
            topic,
            userName,
            password
        )
        mqttHelper.init()

        mqttHelper.mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                Log.w("TrungPQ", "Connected")
            }

            override fun connectionLost(throwable: Throwable) {
                Log.w("TrungPQ", "Connect False")
            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                Log.w("TrungPQ", "Delivered")
            }

            @Throws(java.lang.Exception::class)
            override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                Log.e("TrungPQ", mqttMessage.toString())
                if (mqttMessage.toString() == "null") return
                try {
                    // convert message MQTT
                    val message = Gson().fromJson(
                        mqttMessage.toString(),
                        MessageMQTT::class.java
                    )
                    LogUtils.logInfo("Receiver MQTT message: $message")
                    if (message.menu || message.plateModel || message.product || message.timeBlock) {
                        // sync all data
                        val intent = Intent()
                        intent.action = "MQTT_SYNC_DATA"
                        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                    }
                } catch (ex: Exception) {
                    LogUtils.logError(ex)
                }
            }
        })
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
}