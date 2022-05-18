package com.konbini.magicplateuhf.ui.settings

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.databinding.FragmentSettingsBinding
import com.konbini.magicplateuhf.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    companion object {
        const val TAG = "SettingsFragment"
    }

    private var processing = false
    private var orderStatus: MutableList<String> = mutableListOf()
    private lateinit var adapterOrderStatus: OrderStatusAdapter
    private var binding: FragmentSettingsBinding by autoCleared()
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupDefault()
        setupActions()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        menu.findItem(R.id.action_save).isVisible = true
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (processing) return super.onOptionsItemSelected(item)

        //handle item clicks
        when (item.itemId) {
            R.id.action_save -> {
                LogUtils.logInfo("Click save settings")
                insert()
            }
            R.id.action_sync_all -> {
                LogUtils.logInfo("Click sync all")
                syncAll()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.state.collect() { _state ->
                when (_state.status) {
                    Resource.Status.LOADING -> {
                        showHideLoading(true)
                        processing = true
                    }
                    Resource.Status.SUCCESS -> {
                        showHideLoading(false)
                        processing = false
                        bindOrderStatus()
                        AlertDialogUtil.showSuccess(_state.message, requireContext())
                        LogUtils.logInfo("Sync all success")
                    }
                    Resource.Status.ERROR -> {
                        showHideLoading(false)
                        processing = false
                        AlertDialogUtil.showError(_state.message, requireContext())
                        LogUtils.logInfo("Sync all error")
                    }
                }
            }
        }
    }

    private fun setupDefault() {
        bindCompany()
        bindHardware()
        bindTimer()
        bindMachine()
        bindCloud()
        bindWallet()
        bindMQTT()
        bindAlert()
    }

    private fun setupActions() {
        /**
         * Reset the specified address reader.
         * @param btReadId	Reader Address(0xFF Public Address)
         * @return	Succeeded :0, Failed:-1
         */
        binding.btnResetReader.setSafeOnClickListener {
            try {
                if (!MainApplication.isInitializedUHF()) {
                    AlertDialogUtil.showError(
                        getString(R.string.message_error_reader_uhf_has_not_been_initialized),
                        requireContext()
                    )
                    return@setSafeOnClickListener
                }
                val result = MainApplication.mReaderUHF.reset(0xff.toByte())
                if (result == 0) {
                    AlertDialogUtil.showSuccess(
                        getString(R.string.message_success_reset_reader),
                        requireContext()
                    )
                } else {
                    AlertDialogUtil.showError(
                        getString(R.string.message_error_send_command),
                        requireContext()
                    )
                }

            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }

        /**
         * Set output power(Method 1).
         * <br> This command consumes more than 100mS.
         * <br> If you want you change the output power frequently, please use Cmd_set_temporary_output_power command, which doesn't reduce the life of the internal flash memory.
         * @param btReadId		Reader Address(0xFF Public Address)
         * @param btOutputPower	RF output power, range from 0 to 33(0x00 - 0x21), the unit is dBm.
         * @return	Succeeded :0, Failed:-1
         */
        binding.btnSetRFOutputPower.setSafeOnClickListener {
            try {
                if (!MainApplication.isInitializedUHF()) {
                    AlertDialogUtil.showError(
                        getString(R.string.message_error_reader_uhf_has_not_been_initialized),
                        requireContext()
                    )
                    return@setSafeOnClickListener
                }

                var btOutputPower = 0
                btOutputPower = binding.hardwareRFOutputPower.text.toString().toInt()

                if (btOutputPower < 0 || btOutputPower > 33) {
                    AlertDialogUtil.showError(
                        getString(R.string.message_error_rf_output_power_range),
                        requireContext()
                    )
                    return@setSafeOnClickListener
                }
                val result = MainApplication.mReaderUHF.setOutputPower(0xff.toByte(), btOutputPower.toByte())
                if (result == 0) {
                    PrefUtil.setInt("AppSettings.Hardware.Comport.RFOutputPower", btOutputPower)

                    // Refresh Configuration
                    AppSettings.getAllSetting()

                    AlertDialogUtil.showSuccess(
                        getString(R.string.message_success_set_rf_output_power),
                        requireContext()
                    )
                } else {
                    AlertDialogUtil.showError(
                        getString(R.string.message_error_send_command),
                        requireContext()
                    )
                }

            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }

    // region Binding
    private fun bindCompany() {
        binding.companyLogo.setText(AppSettings.Company.Logo)
        binding.companyName.setText(AppSettings.Company.Name)
        binding.companyTel.setText(AppSettings.Company.Tel)
        binding.companyEmail.setText(AppSettings.Company.Email)
        binding.companyAddress.setText(AppSettings.Company.Address)
    }

    private fun bindHardware() {
        binding.hardwareUhfReader.setText(AppSettings.Hardware.Comport.ReaderUHF)
        binding.hardwareIuc.setText(AppSettings.Hardware.Comport.IUC)
        binding.hardwareRFOutputPower.setText(AppSettings.Hardware.Comport.RFOutputPower.toString())
    }

    private fun bindTimer() {
        val hour = String.format("%02d", AppSettings.Timer.SpecifiedTimeHour)
        binding.specificTimeHour.setText(hour)

        val minute = String.format("%02d", AppSettings.Timer.SpecifiedTimeMinute)
        binding.specificTimeMinute.setText(minute)

        binding.periodicSyncOffline.setText(AppSettings.Timer.PeriodicSyncOffline.toString())
        binding.periodicGetToken.setText(AppSettings.Timer.PeriodicGetToken.toString())

        binding.xDayStoreLocalOrders.setText(AppSettings.Timer.xDayStoreLocalOrders.toString())
        binding.xDayStoreLocalMenus.setText(AppSettings.Timer.xDayStoreLocalMenus.toString())
    }

    private fun bindMachine() {
        binding.machinePinCode.setText(AppSettings.Machine.PinCode)
        binding.machineMacAddress.setText(AppSettings.Machine.MacAddress)
        binding.machineSource.setText(AppSettings.Machine.Source)
        binding.machineTerminal.setText(AppSettings.Machine.Terminal)
        binding.machineStore.setText(AppSettings.Machine.Store)
    }

    private fun bindCloud() {
        binding.cloudHost.setText(AppSettings.Cloud.Host)
        binding.cloudConsumerKey.setText(AppSettings.Cloud.ConsumerKey)
        binding.cloudConsumerSecret.setText(AppSettings.Cloud.ConsumerSecret)
        binding.cloudClientId.setText(AppSettings.Cloud.ClientId)
        binding.cloudClientSecret.setText(AppSettings.Cloud.ClientSecret)
        bindOrderStatus()
        binding.cloudProductIdFroCustomPrice.setText(AppSettings.Cloud.ProductIdForCustomPrice.toString())
    }

    private fun bindOrderStatus() {
        val listOrderStatus = AppSettings.Cloud.AllOrderStatus.split("|").map { it.trim() }
        orderStatus.addAll(listOrderStatus)

        adapterOrderStatus = OrderStatusAdapter(requireContext(), orderStatus.toList())
        binding.cloudOrderStatus.adapter = adapterOrderStatus

        orderStatus.forEachIndexed orderStatus@{ index, _status ->
            if (_status == AppSettings.Cloud.OrderStatus) {
                binding.cloudOrderStatus.setSelection(index)
                return@orderStatus
            }
        }
    }

    private fun bindWallet() {
        binding.walletHost.setText(AppSettings.Wallet.Host)
        binding.walletClientId.setText(AppSettings.Wallet.ClientId)
        binding.walletClientSecret.setText(AppSettings.Wallet.ClientSecret)
    }

    private fun bindMQTT() {
        binding.mqttHost.setText(AppSettings.MQTT.Host)
        binding.mqttUserName.setText(AppSettings.MQTT.UserName)
        binding.mqttPassword.setText(AppSettings.MQTT.Password)
        binding.mqttTopic.setText(AppSettings.MQTT.Topic)
    }

    private fun bindAlert() {
        binding.alertTelegramUserName.setText(AppSettings.Alert.Telegram.UserName)
        binding.alertTelegramToken.setText(AppSettings.Alert.Telegram.Token)
        binding.alertTelegramGroup.setText(AppSettings.Alert.Telegram.Group)

        binding.alertSlackWebhook.setText(AppSettings.Alert.Slack.Webhook)
    }

    private fun insert() {
        val companyLogo = binding.companyLogo.text.toString().trim()
        val companyName = binding.companyName.text.toString().trim()
        val companyTel = binding.companyTel.text.toString().trim()
        val companyEmail = binding.companyEmail.text.toString().trim()
        val companyAddress = binding.companyAddress.text.toString().trim()

        val hardwareIuc = binding.hardwareIuc.text.toString().trim()
        val hardwareUhfReader = binding.hardwareUhfReader.text.toString().trim()

        val machinePinCode = binding.machinePinCode.text.toString().trim()
        val machineMacAddress = binding.machineMacAddress.text.toString().trim()
        val machineSource = binding.machineSource.text.toString().trim()
        val machineTerminal = binding.machineTerminal.text.toString().trim()
        val machineStore = binding.machineStore.text.toString().trim()

        val cloudHost = binding.cloudHost.text.toString().trim()
        val cloudConsumerKey = binding.cloudConsumerKey.text.toString().trim()
        val cloudConsumerSecret = binding.cloudConsumerSecret.text.toString().trim()
        val cloudClientId = binding.cloudClientId.text.toString().trim()
        val cloudClientSecret = binding.cloudClientSecret.text.toString().trim()
        val cloudProductIdFroCustomPrice = binding.cloudProductIdFroCustomPrice.text.toString().trim()

        val walletHost = binding.walletHost.text.toString().trim()
        val walletClientId = binding.walletClientId.text.toString().trim()
        val walletClientSecret = binding.walletClientSecret.text.toString().trim()

        val mqttHost = binding.mqttHost.text.toString().trim()
        val mqttUserName = binding.mqttUserName.text.toString().trim()
        val mqttPassword = binding.mqttPassword.text.toString().trim()
        val mqttTopic = binding.mqttTopic.text.toString().trim()

        val alertTelegramUserName = binding.alertTelegramUserName.text.toString().trim()
        val alertTelegramToken = binding.alertTelegramToken.text.toString().trim()
        val alertTelegramGroup = binding.alertTelegramGroup.text.toString().trim()

        val alertSlackWebhook = binding.alertSlackWebhook.text.toString().trim()

        val hour = binding.specificTimeHour.text.toString().trim()
        val minute = binding.specificTimeMinute.text.toString().trim()

        val periodicSyncOffline = binding.periodicSyncOffline.text.toString().trim()
        val periodicGetToken = binding.periodicGetToken.text.toString().trim()
        val xDayStoreLocalOrders = binding.xDayStoreLocalOrders.text.toString().trim()
        val xDayStoreLocalMenus = binding.xDayStoreLocalMenus.text.toString().trim()

        PrefUtil.setString("AppSettings.Company.Logo", companyLogo)
        PrefUtil.setString("AppSettings.Company.Name", companyName)
        PrefUtil.setString("AppSettings.Company.Tel", companyTel)
        PrefUtil.setString("AppSettings.Company.Email", companyEmail)
        PrefUtil.setString("AppSettings.Company.Address", companyAddress)

        PrefUtil.setString("AppSettings.Hardware.Comport.IUC", hardwareIuc)
        if (AppSettings.Hardware.Comport.IUC != hardwareIuc) {
            MainApplication.initIM30()
        }

        PrefUtil.setString("AppSettings.Hardware.Comport.ReaderUHF", hardwareUhfReader)
        if (AppSettings.Hardware.Comport.ReaderUHF != hardwareUhfReader) {
            MainApplication.initRFIDReaderUHF()
        }

        PrefUtil.setInt("AppSettings.Timer.SpecifiedTimeHour", if (hour.isEmpty()) 0 else hour.toInt())
        PrefUtil.setInt("AppSettings.Timer.SpecifiedTimeMinute", if (minute.isEmpty()) 0 else minute.toInt())

        PrefUtil.setInt(
            "AppSettings.Timer.PeriodicSyncOffline",
            if (periodicSyncOffline.isEmpty()) 0 else periodicSyncOffline.toInt()
        )
        PrefUtil.setInt(
            "AppSettings.Timer.PeriodicGetToken",
            if (periodicGetToken.isEmpty()) 0 else periodicGetToken.toInt()
        )
        PrefUtil.setInt(
            "AppSettings.Timer.xDayStoreLocalOrders",
            if (xDayStoreLocalOrders.isEmpty()) 0 else xDayStoreLocalOrders.toInt()
        )
        PrefUtil.setInt(
            "AppSettings.Timer.xDayStoreLocalMenus",
            if (xDayStoreLocalMenus.isEmpty()) 0 else xDayStoreLocalMenus.toInt()
        )

        PrefUtil.setString("AppSettings.Machine.PinCode", machinePinCode)
        PrefUtil.setString("AppSettings.Machine.MacAddress", machineMacAddress)
        PrefUtil.setString("AppSettings.Machine.Source", machineSource)
        PrefUtil.setString("AppSettings.Machine.Terminal", machineTerminal)
        PrefUtil.setString("AppSettings.Machine.Store", machineStore)

        PrefUtil.setString("AppSettings.Cloud.Host", cloudHost)
        PrefUtil.setString("AppSettings.Cloud.ConsumerKey", cloudConsumerKey)
        PrefUtil.setString("AppSettings.Cloud.ConsumerSecret", cloudConsumerSecret)
        PrefUtil.setString("AppSettings.Cloud.ClientId", cloudClientId)
        PrefUtil.setString("AppSettings.Cloud.ClientSecret", cloudClientSecret)
        PrefUtil.setString("AppSettings.Cloud.OrderStatus", binding.cloudOrderStatus.selectedItem.toString())
        PrefUtil.setInt("AppSettings.Cloud.ProductIdForCustomPrice", cloudProductIdFroCustomPrice.toInt())

        PrefUtil.setString("AppSettings.Wallet.Host", walletHost)
        PrefUtil.setString("AppSettings.Wallet.ClientId", walletClientId)
        PrefUtil.setString("AppSettings.Wallet.ClientSecret", walletClientSecret)

        PrefUtil.setString("AppSettings.MQTT.Host", mqttHost)
        PrefUtil.setString("AppSettings.MQTT.Username", mqttUserName)
        PrefUtil.setString("AppSettings.MQTT.Password", mqttPassword)
        PrefUtil.setString("AppSettings.MQTT.Topic", mqttTopic)

        PrefUtil.setString("AppSettings.Alert.Telegram.UserName", alertTelegramUserName)
        PrefUtil.setString("AppSettings.Alert.Telegram.Token", alertTelegramToken)
        PrefUtil.setString("AppSettings.Alert.Telegram.Group", alertTelegramGroup)

        PrefUtil.setString("AppSettings.Alert.Slack.Webhook", alertSlackWebhook)

        // Refresh Configuration
        AppSettings.getAllSetting()

        AlertDialogUtil.showSuccess(getString(R.string.message_success_save), requireContext())

        processing = false
    }

    private fun syncAll() {
        viewModel.syncAll()
    }

    private fun showHideLoading(show: Boolean) {
        if (show) {
            binding.loadingPanel.visibility = View.VISIBLE
            binding.scrollViewPanel.visibility = View.GONE
        } else {
            binding.loadingPanel.visibility = View.GONE
            binding.scrollViewPanel.visibility = View.VISIBLE
        }
    }
}