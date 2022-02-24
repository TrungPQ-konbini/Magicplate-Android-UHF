package com.konbini.magicplateuhf.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.databinding.ActivitySalesBinding
import com.konbini.magicplateuhf.ui.plateModel.PlateModelViewModel
import com.konbini.magicplateuhf.utils.LogUtils
import com.rfid.rxobserver.RXObserver
import com.rfid.rxobserver.bean.RXInventoryTag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class SalesActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SalesActivity"
    }

//    private var test = false
    private var listEPC: MutableList<String> = mutableListOf()

    private var rxObserver: RXObserver = object : RXObserver() {
        override fun onInventoryTag(tag: RXInventoryTag) {
            Log.d(TAG, tag.strEPC)
            listEPC.add(tag.strEPC.replace("\\s".toRegex(), ""))
        }

        override fun onInventoryTagEnd(endTag: RXInventoryTag.RXInventoryTagEnd) {
//            if (test) listEPC.removeFirst()
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
                    MainApplication.mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
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
//        Timer().schedule(object : TimerTask() {
//            override fun run() {
//                test = true
//                Log.e(TAG, "Start Alarm")
//            }
//        }, 15000)
//
//        Timer().schedule(object : TimerTask() {
//            override fun run() {
//                test = false
//                Log.e(TAG, "Stop Alarm")
//            }
//        }, 25000)
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
            if (MainApplication.isInitializedUHF) {
                // Start reading UHF
                MainApplication.mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
            }
        }
    }

    override fun onDestroy() {
        if (MainApplication.isInitializedUHF) {
            MainApplication.mReaderUHF.unRegisterObserver(rxObserver)
        }
        super.onDestroy()
    }

    private fun initRFIDReader() {
        try {
            MainApplication.mReaderUHF.registerObserver(rxObserver)
            Thread.sleep(500)
            MainApplication.mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
        } catch (ex: Exception) {
            Log.e(TAG, ex.toString())
            LogUtils.logError(ex)
        }
    }
}