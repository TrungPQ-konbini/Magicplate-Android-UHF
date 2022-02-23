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
            PrefUtil.setBoolean("AppSettings.Options.Payment.Ezlink", isChecked)
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

        binding.checkboxSyncOrderRealtime.setOnCheckedChangeListener { buttonView, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.SyncOrderRealtime", isChecked)
            LogUtils.logInfo("AppSettings.Options.SyncOrderRealtime Options: $isChecked")
            showMessageSuccess()
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

        binding.checkboxAlertTelegramActivated.isChecked = AppSettings.Alert.Telegram.Activated
        binding.checkboxAlertSlackActivated.isChecked = AppSettings.Alert.Slack.Activated

        binding.checkboxNotAllowWalletNonRfidActivated.isChecked = AppSettings.Options.NotAllowWalletNonRfid
        binding.checkboxSyncOrderRealtime.isChecked = AppSettings.Options.SyncOrderRealtime
        binding.checkboxCancelPayment.isChecked = AppSettings.Options.AllowAdminCancelPayment

        binding.keyCodeCancelPayment.text = AppSettings.Options.KeyCodeCancelPayment
    }

    private fun showMessageSuccess() {
        AlertDialogUtil.showSuccess(getString(R.string.message_success_save), requireContext())

        // Refresh Configuration
        AppSettings.getAllSetting()
    }
}