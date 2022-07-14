package com.konbini.magicplateuhf.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
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
import com.konbini.magicplateuhf.utils.SafeClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                R.id.nav_users,
                R.id.nav_discounts,
                R.id.nav_options_setting,
                R.id.nav_settings,
                R.id.nav_diagnostic_tags,
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

        val layoutRestart: LinearLayout = hView.findViewById(R.id.layout_restart)
        layoutRestart.setSafeOnClickListener {
            AppContainer.GlobalVariable.actionRestartShutdown = "RESTART"
            val intent = Intent(this, SalesActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        val layoutShutdown: LinearLayout = hView.findViewById(R.id.layout_shutdown)
        layoutShutdown.setSafeOnClickListener {
            AppContainer.GlobalVariable.actionRestartShutdown = "SHUTDOWN"
            val intent = Intent(this, SalesActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        AppContainer.GlobalVariable.isBackend = true
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
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
        super.onDestroy()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN) {
            var pressedKey = e.displayLabel.toString()
            when (e.keyCode) {
                KeyEvent.KEYCODE_NUM_LOCK -> { pressedKey = "KEYCODE_NUM_LOCK" }
                KeyEvent.KEYCODE_NUMPAD_0 -> { pressedKey = "KEYCODE_NUMPAD_0" }
                KeyEvent.KEYCODE_NUMPAD_1 -> { pressedKey = "KEYCODE_NUMPAD_1" }
                KeyEvent.KEYCODE_NUMPAD_2 -> { pressedKey = "KEYCODE_NUMPAD_2" }
                KeyEvent.KEYCODE_NUMPAD_3 -> { pressedKey = "KEYCODE_NUMPAD_3" }
                KeyEvent.KEYCODE_NUMPAD_4 -> { pressedKey = "KEYCODE_NUMPAD_4" }
                KeyEvent.KEYCODE_NUMPAD_5 -> { pressedKey = "KEYCODE_NUMPAD_5" }
                KeyEvent.KEYCODE_NUMPAD_6 -> { pressedKey = "KEYCODE_NUMPAD_6" }
                KeyEvent.KEYCODE_NUMPAD_7 -> { pressedKey = "KEYCODE_NUMPAD_7" }
                KeyEvent.KEYCODE_NUMPAD_8 -> { pressedKey = "KEYCODE_NUMPAD_8" }
                KeyEvent.KEYCODE_NUMPAD_9 -> { pressedKey = "KEYCODE_NUMPAD_9" }
                KeyEvent.KEYCODE_NUMPAD_DIVIDE -> { pressedKey = "KEYCODE_NUMPAD_DIVIDE" }
                KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> { pressedKey = "KEYCODE_NUMPAD_MULTIPLY" }
                KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> { pressedKey = "KEYCODE_NUMPAD_SUBTRACT" }
                KeyEvent.KEYCODE_NUMPAD_ADD -> { pressedKey = "KEYCODE_NUMPAD_ADD" }
                KeyEvent.KEYCODE_NUMPAD_DOT -> { pressedKey = "KEYCODE_NUMPAD_DOT" }
                KeyEvent.KEYCODE_NUMPAD_COMMA -> { pressedKey = "KEYCODE_NUMPAD_COMMA" }
                KeyEvent.KEYCODE_NUMPAD_ENTER -> { pressedKey = "KEYCODE_NUMPAD_ENTER" }
                KeyEvent.KEYCODE_NUMPAD_EQUALS -> { pressedKey = "KEYCODE_NUMPAD_EQUALS" }
            }
            if (pressedKey != "��" && e.keyCode != 124) {
                val intent = Intent()
                intent.action = "KEY_CODE"
                intent.putExtra("pressedKey", pressedKey)
                LocalBroadcastManager.getInstance(MainApplication.instance.applicationContext).sendBroadcast(intent)
            }
        }
        return super.dispatchKeyEvent(e)
    }
}