package com.konbini.magicplateuhf.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
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
        private var defaultInterval: Int = 1000
        private var lastTimeClicked: Long = 0
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
            //if (refresh) {
                // Send Broadcast to update UI

                val intent = Intent()
                intent.action = "REFRESH_TAGS"
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

                if (AppContainer.InitData.allowReadTags) {
                    // Start reading UHF
                    MainApplication.mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
                }
            //}

            AppContainer.CurrentTransaction.oldListTagEntity = listTagEntity
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

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN) {
            val pressedKey = e.unicodeChar.toChar()
            AppContainer.CurrentTransaction.barcode += pressedKey
        }
        Log.e("KEY_CODE", e.keyCode.toString())
        if (e.action == KeyEvent.ACTION_DOWN) {
            when (e.keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
                        return super.dispatchKeyEvent(e)
                    }
                    lastTimeClicked = SystemClock.elapsedRealtime()
                    Log.e("BARCODE_VALUE", AppContainer.CurrentTransaction.barcode)
                    val intent = Intent()
                    intent.action = "NEW_BARCODE"
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                }
            }
        }
        return super.dispatchKeyEvent(e)
    }
}