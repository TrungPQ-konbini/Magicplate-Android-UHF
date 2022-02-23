package com.konbini.magicplateuhf.ui.settings

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.konbini.magicplateuhf.AppSettings
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
        bindMachine()
        bindCloud()
        bindWallet()
        bindMQTT()
        bindAlert()
    }

    private fun setupActions() {

    }

    // region Binding
    private fun bindCompany() {
        binding.companyLogo.setText(AppSettings.Company.Logo)
        binding.companyName.setText(AppSettings.Company.Name)
        binding.companyTel.setText(AppSettings.Company.Tel)
        binding.companyEmail.setText(AppSettings.Company.Email)
        binding.companyAddress.setText(AppSettings.Company.Address)
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

        PrefUtil.setString("AppSettings.Company.Logo", companyLogo)
        PrefUtil.setString("AppSettings.Company.Name", companyName)
        PrefUtil.setString("AppSettings.Company.Tel", companyTel)
        PrefUtil.setString("AppSettings.Company.Email", companyEmail)
        PrefUtil.setString("AppSettings.Company.Address", companyAddress)

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