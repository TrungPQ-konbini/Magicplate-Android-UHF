package com.konbini.magicplateuhf.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.ui.*
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.databinding.ActivityMainBinding
import com.konbini.magicplateuhf.utils.LogUtils
import com.rfid.rxobserver.RXObserver
import com.rfid.rxobserver.bean.RXInventoryTag
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private var listEPC: MutableList<String> = mutableListOf()

    private var rxObserver: RXObserver = object : RXObserver() {
        override fun onInventoryTag(tag: RXInventoryTag) {
            Log.d(SalesActivity.TAG, tag.strEPC)
            listEPC.add(tag.strEPC.replace("\\s".toRegex(), ""))
        }

        override fun onInventoryTagEnd(endTag: RXInventoryTag.RXInventoryTagEnd) {
            AppContainer.CurrentTransaction.listEPC.clear()
            AppContainer.CurrentTransaction.listEPC.addAll(listEPC)

            // Get list tags
            val listTagEntity = AppContainer.GlobalVariable.getListTagEntity(listEPC)
            AppContainer.CurrentTransaction.listTagEntity = listTagEntity

            // Send Broadcast to update UI
            val intent = Intent()
            intent.action = "REFRESH_TAGS"
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

            if (!AppContainer.GlobalVariable.allowWriteTags && MainApplication.isInitializedUHF) {
                // Start reading UHF
                MainApplication.mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
            }

            listEPC.clear()
        }
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRFIDReader()

        setSupportActionBar(binding.appBarMain.toolbar)

//        // TODO: TrungPQ delete button email
//        binding.appBarMain.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navView.itemIconTintList = null
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // TrungPQ set version.
        val hView: View = navView.getHeaderView(0)
        val currentVersion: TextView = hView.findViewById(R.id.currentVersion)
        currentVersion.text = MainApplication.currentVersion

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_time_blocks,
                R.id.nav_menus,
                R.id.nav_plate_models,
                R.id.nav_categories,
                R.id.nav_products,
                R.id.nav_transactions,
                R.id.nav_options_setting,
                R.id.nav_settings,
                R.id.nav_write_tags,
                R.id.nav_register_tags,
                R.id.nav_logout
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Add logout navigation
        navView.setNavigationItemSelectedListener {
            val handled = NavigationUI.onNavDestinationSelected(it, navController)
            when (it.itemId) {
                R.id.nav_logout -> {
                    logout()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            handled
        }
    }

    private fun logout() {
        val intent = Intent(this, SalesActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

//    // TODO: TrungPQ delete button OptionsMenu
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
//        return true
//    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
//        if (MainApplication.isInitializedUHF) {
//            MainApplication.mReaderUHF.unRegisterObserver(rxObserver)
//        }
        super.onDestroy()
    }

    private fun initRFIDReader() {
        try {
            MainApplication.mReaderUHF.registerObserver(rxObserver)
            Thread.sleep(500)
            MainApplication.mReaderUHF.realTimeInventory(0xff.toByte(), 0x01.toByte())
        } catch (ex: Exception) {
            Log.e(SalesActivity.TAG, ex.toString())
            LogUtils.logError(ex)
        }
    }
}