package com.konbini.magicplateuhf.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.entities.PlateModelEntity
import com.konbini.magicplateuhf.data.entities.TagEntity
import com.konbini.magicplateuhf.databinding.ActivitySalesBinding
import com.konbini.magicplateuhf.ui.plateModel.PlateModelViewModel
import com.konbini.magicplateuhf.utils.CommonUtil
import com.konbini.magicplateuhf.utils.LogUtils
import com.module.interaction.ModuleConnector
import com.nativec.tools.ModuleManager
import com.rfid.RFIDReaderHelper
import com.rfid.ReaderConnector
import com.rfid.rxobserver.RXObserver
import com.rfid.rxobserver.bean.RXInventoryTag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SalesActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SalesActivity"
    }

    private var listEPC: MutableList<String> = mutableListOf()

    var connector: ModuleConnector = ReaderConnector()
    lateinit var mReader: RFIDReaderHelper

    private var rxObserver: RXObserver = object : RXObserver() {
        override fun onInventoryTag(tag: RXInventoryTag) {
            Log.d(TAG, tag.strEPC)
            listEPC.add(tag.strEPC.replace("\\s".toRegex(), ""))
        }

        override fun onInventoryTagEnd(endTag: RXInventoryTag.RXInventoryTagEnd) {
            AppContainer.CurrentTransaction.listEPC.clear()
            AppContainer.CurrentTransaction.listEPC.addAll(listEPC)
            // Get list tags
            val listTagEntity = AppContainer.InitData.getListTagEntity(listEPC)
            AppContainer.CurrentTransaction.listTagEntity = listTagEntity
            // Add items to cart
            val refresh = AppContainer.CurrentTransaction.refreshCart()
            if (refresh) {
                // Send Broadcast to update UI
                val intent = Intent()
                intent.action = "REFRESH_TAGS"
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

                if (!AppContainer.InitData.allowWriteTags) {
                    // Start reading UHF
                    mReader.realTimeInventory(0xff.toByte(), 0x01.toByte())
                }
            }
            listEPC.clear()
        }
    }

    private lateinit var binding: ActivitySalesBinding
    private val viewModelPlateModel: PlateModelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRFIDReader()

        // Get all plate model
        lifecycleScope.launch {
            AppContainer.InitData.listPlatesModel = viewModelPlateModel.getAll().toMutableList()
        }

//        // TODO: TrungPQ add to test
//        Timer().schedule(object : TimerTask() {
//            override fun run() {
//                trungpqTest()
//            }
//        }, 5000)
    }

    private fun trungpqTest() {
        listEPC.add("40 00 2D 2C AD F8 94 D1 B3 40 50 D0".replace("\\s".toRegex(), ""))
        listEPC.add("40 00 2F 2C AD F8 94 D1 B3 40 50 D0".replace("\\s".toRegex(), ""))
        AppContainer.CurrentTransaction.listEPC.clear()
        AppContainer.CurrentTransaction.listEPC.addAll(listEPC)
        // Get list tags
        val listTagEntity = AppContainer.InitData.getListTagEntity(listEPC)
        AppContainer.CurrentTransaction.listTagEntity = listTagEntity
        // Add items to cart
        val refresh = AppContainer.CurrentTransaction.refreshCart()
        if (refresh) {
            // Send Broadcast to update UI
            val intent = Intent()
            intent.action = "REFRESH_TAGS"
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            if (this::mReader.isInitialized && mReader != null) {
                // Start reading UHF
                mReader.realTimeInventory(0xff.toByte(), 0x01.toByte())
            }
        }
    }

    override fun onDestroy() {
        if (this::mReader.isInitialized && mReader != null) {
            mReader.unRegisterObserver(rxObserver)
        }
        if (connector != null) {
            connector.disConnect()
        }

        ModuleManager.newInstance().uhfStatus = false
        ModuleManager.newInstance().release()

        super.onDestroy()
    }

    private fun initRFIDReader() {
        try {
            if (connector.connectCom(
                    AppSettings.Machine.ReaderUHF,
                    AppSettings.Machine.ReaderUHFBaudRate
                )
            ) {
                ModuleManager.newInstance().uhfStatus = true
                try {
                    mReader = RFIDReaderHelper.getDefaultHelper()
                    mReader.registerObserver(rxObserver)
                    Thread.sleep(500)
                    mReader.realTimeInventory(0xff.toByte(), 0x01.toByte())
                } catch (ex: Exception) {
                    Log.e(TAG, ex.toString())
                    LogUtils.logError(ex)
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, ex.toString())
            LogUtils.logError(ex)
        }
    }
}