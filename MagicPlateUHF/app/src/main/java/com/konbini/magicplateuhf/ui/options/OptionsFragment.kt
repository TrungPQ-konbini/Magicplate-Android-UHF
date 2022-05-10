package com.konbini.magicplateuhf.ui.options

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.enum.AcsReaderType
import com.konbini.magicplateuhf.data.enum.MachineType
import com.konbini.magicplateuhf.databinding.FragmentOptionsBinding
import com.konbini.magicplateuhf.utils.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OptionsFragment : Fragment() {

    companion object {
        const val TAG = "OptionsFragment"
    }

    private var binding: FragmentOptionsBinding by autoCleared()
    private val viewModel: OptionsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        setupActions()
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun setupActions() {
        binding.checkboxCustomerUi.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setString(
                    "AppSettings.Options.MachineTypeActivated",
                    MachineType.MAGIC_PLATE.value
                )
                binding.checkboxCustomerUi.isChecked = true
                binding.checkboxSelfKiosk.isChecked = false

                // Reset
                AppContainer.CurrentTransaction.resetTemporaryInfo()

                showMessageSuccess()
            } else {
                binding.checkboxCustomerUi.isChecked = false
                binding.checkboxSelfKiosk.isChecked = true
            }
        }

        binding.checkboxSelfKiosk.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setString(
                    "AppSettings.Options.MachineTypeActivated",
                    MachineType.SELF_KIOSK.value
                )
                binding.checkboxCustomerUi.isChecked = false
                binding.checkboxSelfKiosk.isChecked = true

                // Reset
                AppContainer.CurrentTransaction.resetTemporaryInfo()

                showMessageSuccess()
            } else {
                binding.checkboxCustomerUi.isChecked = true
                binding.checkboxSelfKiosk.isChecked = false
            }
        }

        binding.checkboxMasterCard.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.MasterCard", isChecked)
            LogUtils.logInfo("Payment mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxEzlink.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.EzLink", isChecked)
            LogUtils.logInfo("Payment mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxWallet.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.Wallet", isChecked)
            LogUtils.logInfo("Payment mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxPayNow.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.PayNow", isChecked)
            LogUtils.logInfo("Payment PayNow mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxWhiteAscReader.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setString("AppSettings.Options.AcsReader", AcsReaderType.WHITE.value)
                binding.checkboxWhiteAscReader.isChecked = true
                binding.checkboxBlackAscReader.isChecked = false
                showMessageSuccess()
            } else {
                binding.checkboxWhiteAscReader.isChecked = false
                binding.checkboxBlackAscReader.isChecked = true
            }
        }

        binding.checkboxBlackAscReader.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setString("AppSettings.Options.AcsReader", AcsReaderType.BLACK.value)
                binding.checkboxWhiteAscReader.isChecked = false
                binding.checkboxBlackAscReader.isChecked = true
                showMessageSuccess()
            } else {
                binding.checkboxWhiteAscReader.isChecked = true
                binding.checkboxBlackAscReader.isChecked = false
            }
        }

        binding.checkboxPrinterBle.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Printer.Bluetooth", isChecked)
                PrefUtil.setBoolean("AppSettings.Options.Printer.TCP", false)
                PrefUtil.setBoolean("AppSettings.Options.Printer.USB", false)

                binding.checkboxPrinterTcp.isChecked = false
                binding.checkboxPrinterUsb.isChecked = false
            } else {
                if (!binding.checkboxPrinterTcp.isChecked && !binding.checkboxPrinterUsb.isChecked) {
                    binding.checkboxPrinterBle.isChecked = true
                }
            }
        }

        binding.checkboxPrinterTcp.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Printer.Bluetooth", false)
                PrefUtil.setBoolean("AppSettings.Options.Printer.TCP", isChecked)
                PrefUtil.setBoolean("AppSettings.Options.Printer.USB", false)

                binding.checkboxPrinterBle.isChecked = false
                binding.checkboxPrinterUsb.isChecked = false
            } else {
                if (!binding.checkboxPrinterBle.isChecked && !binding.checkboxPrinterUsb.isChecked) {
                    binding.checkboxPrinterTcp.isChecked = true
                }
            }
        }

        binding.checkboxPrinterUsb.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Printer.Bluetooth", false)
                PrefUtil.setBoolean("AppSettings.Options.Printer.TCP", false)
                PrefUtil.setBoolean("AppSettings.Options.Printer.USB", isChecked)

                binding.checkboxPrinterBle.isChecked = false
                binding.checkboxPrinterTcp.isChecked = false
            } else {
                if (!binding.checkboxPrinterBle.isChecked && !binding.checkboxPrinterTcp.isChecked) {
                    binding.checkboxPrinterUsb.isChecked = true
                }
            }
        }

        binding.checkboxAlertTelegramActivated.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Alert.Telegram.Activated", isChecked)
            LogUtils.logInfo("AppSettings.Alert.Telegram.Activated Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxAlertSlackActivated.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Alert.Slack.Activated", isChecked)
            LogUtils.logInfo("AppSettings.Alert.Slack.Activated Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxNotAllowWalletNonRfidActivated.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.NotAllowWalletNonRfid", isChecked)
            LogUtils.logInfo("AppSettings.Options.NotAllowWalletNonRfid Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxIgnoreWhenRemovingTags.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.IgnoreWhenRemovingTags", isChecked)
            LogUtils.logInfo("AppSettings.Options.IgnoreWhenRemovingTags Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxSyncOrderRealtime.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.checkboxSyncOrderPeriodicPerTimePeriod.isChecked = false
                binding.checkboxSyncSpecifiedTime.isChecked = false
                binding.checkboxNoSyncOrder.isChecked = false
                showMessageSuccess()
            }
            PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderRealtime", isChecked)
            LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderRealtime Options: $isChecked")
        }

        binding.checkboxSyncOrderPeriodicPerTimePeriod.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.checkboxSyncOrderRealtime.isChecked = false
                binding.checkboxSyncSpecifiedTime.isChecked = false
                binding.checkboxNoSyncOrder.isChecked = false
                showMessageSuccess()
            }
            PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod", isChecked)
            LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod Options: $isChecked")
        }

        binding.checkboxSyncSpecifiedTime.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.checkboxSyncOrderRealtime.isChecked = false
                binding.checkboxSyncOrderPeriodicPerTimePeriod.isChecked = false
                binding.checkboxNoSyncOrder.isChecked = false
                showMessageSuccess()
            }
            PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderSpecifiedTime", isChecked)
            LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderSpecifiedTime Options: $isChecked")
        }

        binding.checkboxNoSyncOrder.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.checkboxSyncOrderRealtime.isChecked = false
                binding.checkboxSyncOrderPeriodicPerTimePeriod.isChecked = false
                binding.checkboxSyncSpecifiedTime.isChecked = false
                showMessageSuccess()
            }
            PrefUtil.setBoolean("AppSettings.Options.Sync.NoSyncOrder", isChecked)
            LogUtils.logInfo("AppSettings.Options.Sync.NoSyncOrder Options: $isChecked")
        }

        binding.checkboxCancelPayment.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.AllowAdminCancelPayment", isChecked)
            LogUtils.logInfo("AppSettings.Options.AllowAdminCancelPayment Options: $isChecked")
            showMessageSuccess()
        }
    }

    private fun initData() {
        when (AppSettings.Options.MachineTypeActivated) {
            MachineType.MAGIC_PLATE.value -> {
                binding.checkboxCustomerUi.isChecked = true
                binding.checkboxSelfKiosk.isChecked = false
            }
            MachineType.SELF_KIOSK.value -> {
                binding.checkboxCustomerUi.isChecked = false
                binding.checkboxSelfKiosk.isChecked = true
            }
            MachineType.POS.value -> {

            }
        }

        binding.checkboxMasterCard.isChecked = AppSettings.Options.Payment.MasterCard
        binding.checkboxEzlink.isChecked = AppSettings.Options.Payment.EzLink
        binding.checkboxWallet.isChecked = AppSettings.Options.Payment.Wallet
        binding.checkboxPayNow.isChecked = AppSettings.Options.Payment.PayNow

        when (AppSettings.Options.AcsReader) {
            AcsReaderType.WHITE.value -> {
                binding.checkboxWhiteAscReader.isChecked = true
                binding.checkboxBlackAscReader.isChecked = false
            }
            AcsReaderType.BLACK.value -> {
                binding.checkboxWhiteAscReader.isChecked = false
                binding.checkboxBlackAscReader.isChecked = true
            }
        }

        binding.checkboxPrinterBle.isChecked = AppSettings.Options.Printer.Bluetooth
        binding.checkboxPrinterTcp.isChecked = AppSettings.Options.Printer.TCP
        binding.checkboxPrinterUsb.isChecked = AppSettings.Options.Printer.USB

        binding.checkboxAlertTelegramActivated.isChecked = AppSettings.Alert.Telegram.Activated
        binding.checkboxAlertSlackActivated.isChecked = AppSettings.Alert.Slack.Activated

        binding.checkboxNotAllowWalletNonRfidActivated.isChecked = AppSettings.Options.NotAllowWalletNonRfid
        binding.checkboxIgnoreWhenRemovingTags.isChecked = AppSettings.Options.IgnoreWhenRemovingTags

        binding.checkboxSyncOrderRealtime.isChecked = AppSettings.Options.Sync.SyncOrderRealtime
        binding.checkboxSyncOrderPeriodicPerTimePeriod.isChecked = AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod
        binding.checkboxSyncSpecifiedTime.isChecked = AppSettings.Options.Sync.SyncOrderSpecifiedTime
        binding.checkboxNoSyncOrder.isChecked = AppSettings.Options.Sync.NoSyncOrder

        binding.checkboxCancelPayment.isChecked = AppSettings.Options.AllowAdminCancelPayment

        binding.keyCodeCancelPayment.text = AppSettings.Options.KeyCodeCancelPayment
    }

    private fun showMessageSuccess() {
        AlertDialogUtil.showSuccess(getString(R.string.message_success_save), requireContext())

        // Refresh Configuration
        AppSettings.getAllSetting()
    }
}