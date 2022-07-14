package com.konbini.magicplateuhf.ui.options

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.enum.AcsReaderType
import com.konbini.magicplateuhf.data.enum.MachineType
import com.konbini.magicplateuhf.data.enum.PaymentDeviceType
import com.konbini.magicplateuhf.data.enum.PaymentModeType
import com.konbini.magicplateuhf.databinding.FragmentOptionsBinding
import com.konbini.magicplateuhf.utils.*
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class OptionsFragment : Fragment() {

    companion object {
        const val TAG = "OptionsFragment"
        const val PERMISSION_CODE_READ = 1001
        const val IMAGE_PICK_CODE = 1000
    }

    private var accessType = ""

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
        //====================Machine type================================
        binding.checkboxMagicPlateMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setString(
                    "AppSettings.Options.MachineTypeActivated",
                    MachineType.MAGIC_PLATE_MODE.value
                )
                binding.checkboxMagicPlateMode.isChecked = true
                binding.checkboxSelfKioskMode.isChecked = false
                binding.checkboxDiscount.isChecked = false
                binding.checkboxDiscountMode.isChecked = false

                // Reset
                AppContainer.CurrentTransaction.resetTemporaryInfo()

                showMessageSuccess()
            }
        }

        binding.checkboxSelfKioskMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setString(
                    "AppSettings.Options.MachineTypeActivated",
                    MachineType.SELF_KIOSK_MODE.value
                )
                binding.checkboxMagicPlateMode.isChecked = false
                binding.checkboxSelfKioskMode.isChecked = true
                binding.checkboxDiscount.isChecked = false
                binding.checkboxDiscountMode.isChecked = false

                // Reset
                AppContainer.CurrentTransaction.resetTemporaryInfo()

                showMessageSuccess()
            }
        }

        binding.checkboxDiscountMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setString(
                    "AppSettings.Options.MachineTypeActivated",
                    MachineType.DISCOUNT_MODE.value
                )
                binding.checkboxMagicPlateMode.isChecked = false
                binding.checkboxSelfKioskMode.isChecked = false
                binding.checkboxDiscount.isChecked = true
                binding.checkboxDiscountMode.isChecked = true

                // Reset
                AppContainer.CurrentTransaction.resetTemporaryInfo()

                showMessageSuccess()
            }
        }

        //====================Payment device type================================
        binding.checkboxPaymentDeviceTypeIuc.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setString("AppSettings.Options.Payment.DeviceType", PaymentDeviceType.IUC.value)
                binding.checkboxPaymentDeviceTypeIuc.isChecked = true
                binding.checkboxPaymentDeviceTypeIm30.isChecked = false
                showMessageSuccess()
            } else {
                binding.checkboxPaymentDeviceTypeIuc.isChecked = false
                binding.checkboxPaymentDeviceTypeIm30.isChecked = true
            }
        }

        binding.checkboxPaymentDeviceTypeIm30.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setString("AppSettings.Options.Payment.DeviceType", PaymentDeviceType.IM30.value)
                binding.checkboxPaymentDeviceTypeIuc.isChecked = false
                binding.checkboxPaymentDeviceTypeIm30.isChecked = true
                showMessageSuccess()
            } else {
                binding.checkboxPaymentDeviceTypeIuc.isChecked = true
                binding.checkboxPaymentDeviceTypeIm30.isChecked = false
            }
        }

        //====================Payment device mode================================
        binding.checkboxMasterCard.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.MasterCard", isChecked)
            LogUtils.logInfo("Payment mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.buttonAddImageMasterCard.setSafeOnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED)
            ) {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
                requestPermissions(permission, PERMISSION_CODE_READ)
            } else {
                accessType = PaymentModeType.MASTER_CARD.value
                pickImageFromGallery()
            }
        }
        binding.btnClearImageMasterCard.setSafeOnClickListener {
            if (AppSettings.Options.Payment.pathImageMasterCard.isNotEmpty()) {
                binding.imageMasterCardDefault.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_mastercard
                    )
                )
                accessType = PaymentModeType.MASTER_CARD.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxEzlink.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.EzLink", isChecked)
            LogUtils.logInfo("Payment mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.buttonAddImageEzlink.setSafeOnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED)
            ) {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
                requestPermissions(permission, PERMISSION_CODE_READ)
            } else {
                accessType = PaymentModeType.EZ_LINK.value
                pickImageFromGallery()
            }
        }
        binding.btnClearImageEzlink.setSafeOnClickListener {
            if (AppSettings.Options.Payment.pathImageEzLink.isNotEmpty()) {
                binding.imageEzLinkDefault.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_ez_link
                    )
                )
                accessType = PaymentModeType.EZ_LINK.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxWallet.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.Wallet", isChecked)
            LogUtils.logInfo("Payment mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.buttonAddImageWallet.setSafeOnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED)
            ) {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
                requestPermissions(permission, PERMISSION_CODE_READ)
            } else {
                accessType = PaymentModeType.KONBINI_WALLET.value
                pickImageFromGallery()
            }
        }
        binding.btnClearImageWallet.setSafeOnClickListener {
            if (AppSettings.Options.Payment.pathImageWallet.isNotEmpty()) {
                binding.imageWalletDefault.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.logo
                    )
                )
                accessType = PaymentModeType.KONBINI_WALLET.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxPayNow.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.PayNow", isChecked)
            LogUtils.logInfo("Payment PayNow mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.buttonAddImagePayNow.setSafeOnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED)
            ) {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
                requestPermissions(permission, PERMISSION_CODE_READ)
            } else {
                accessType = PaymentModeType.PAY_NOW.value
                pickImageFromGallery()
            }
        }
        binding.btnClearImagePayNow.setSafeOnClickListener {
            if (AppSettings.Options.Payment.pathImagePayNow.isNotEmpty()) {
                binding.imagePayNowDefault.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_pay_now
                    )
                )
                accessType = PaymentModeType.PAY_NOW.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxCash.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.Cash", isChecked)
            LogUtils.logInfo("Payment Cash mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.buttonAddImageCash.setSafeOnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED)
            ) {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
                requestPermissions(permission, PERMISSION_CODE_READ)
            } else {
                accessType = PaymentModeType.CASH.value
                pickImageFromGallery()
            }
        }
        binding.btnClearImageCash.setSafeOnClickListener {
            if (AppSettings.Options.Payment.pathImageCash.isNotEmpty()) {
                binding.imageCashDefault.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_money
                    )
                )
                accessType = PaymentModeType.CASH.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxSelectProduct.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.SelectProduct", isChecked)
            LogUtils.logInfo("Payment SelectProduct mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.buttonAddImageSelectProduct.setSafeOnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED)
            ) {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
                requestPermissions(permission, PERMISSION_CODE_READ)
            } else {
                accessType = PaymentModeType.SELECT_PRODUCT.value
                pickImageFromGallery()
            }
        }
        binding.btnClearImageSelectProduct.setSafeOnClickListener {
            if (AppSettings.Options.Payment.pathImageSelectProduct.isNotEmpty()) {
                binding.imageSelectProductDefault.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_exchange
                    )
                )
                accessType = PaymentModeType.SELECT_PRODUCT.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxTopUp.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Payment.TopUp", isChecked)
            LogUtils.logInfo("Payment TopUp mode Options: $isChecked")
            showMessageSuccess()
        }

        binding.buttonAddImageTopUp.setSafeOnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED)
            ) {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
                requestPermissions(permission, PERMISSION_CODE_READ)
            } else {
                accessType = PaymentModeType.TOP_UP.value
                pickImageFromGallery()
            }
        }
        binding.btnClearImageTopUp.setSafeOnClickListener {
            if (AppSettings.Options.Payment.pathImageTopUp.isNotEmpty()) {
                binding.imageTopUpDefault.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_top_up
                    )
                )
                accessType = PaymentModeType.TOP_UP.value
                saveOptionsPayment("")
            }
        }

        binding.buttonAddImageDiscount.setSafeOnClickListener {
            if ((ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED)
            ) {
                val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

                // GIVE AN INTEGER VALUE FOR PERMISSION_CODE_READ LIKE 1001
                requestPermissions(permission, PERMISSION_CODE_READ)
            } else {
                accessType = PaymentModeType.DISCOUNT.value
                pickImageFromGallery()
            }
        }
        binding.btnClearImageDiscount.setSafeOnClickListener {
            if (AppSettings.Options.Payment.pathImageDiscount.isNotEmpty()) {
                binding.imageDiscountDefault.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_discounts
                    )
                )
                accessType = PaymentModeType.DISCOUNT.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxWhiteAscReader.setOnCheckedChangeListener { _, isChecked ->
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

        binding.checkboxBlackAscReader.setOnCheckedChangeListener { _, isChecked ->
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

        binding.checkboxPrinterBle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Printer.Bluetooth", isChecked)
                PrefUtil.setBoolean("AppSettings.Options.Printer.TCP", false)
                PrefUtil.setBoolean("AppSettings.Options.Printer.USB", false)

                binding.checkboxPrinterTcp.isChecked = false
                binding.checkboxPrinterUsb.isChecked = false
                showMessageSuccess()
            }
        }

        binding.checkboxPrinterTcp.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Printer.Bluetooth", false)
                PrefUtil.setBoolean("AppSettings.Options.Printer.TCP", isChecked)
                PrefUtil.setBoolean("AppSettings.Options.Printer.USB", false)

                binding.checkboxPrinterBle.isChecked = false
                binding.checkboxPrinterUsb.isChecked = false
                showMessageSuccess()
            }
        }

        binding.checkboxPrinterUsb.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Printer.Bluetooth", false)
                PrefUtil.setBoolean("AppSettings.Options.Printer.TCP", false)
                PrefUtil.setBoolean("AppSettings.Options.Printer.USB", isChecked)

                binding.checkboxPrinterBle.isChecked = false
                binding.checkboxPrinterTcp.isChecked = false
                showMessageSuccess()
            }
        }

        binding.checkboxNfc.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Discount.NFC", true)
                PrefUtil.setBoolean("AppSettings.Options.Discount.Barcode", false)
                binding.checkboxNfc.isChecked = true
                binding.checkboxBarcode.isChecked = false
                showMessageSuccess()
            }
        }

        binding.checkboxBarcode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Discount.NFC", false)
                PrefUtil.setBoolean("AppSettings.Options.Discount.Barcode", true)
                binding.checkboxNfc.isChecked = false
                binding.checkboxBarcode.isChecked = true
                showMessageSuccess()
            }
        }

        binding.checkboxAlertTelegramActivated.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Alert.Telegram.Activated", isChecked)
            LogUtils.logInfo("AppSettings.Alert.Telegram.Activated Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxAlertSlackActivated.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Alert.Slack.Activated", isChecked)
            LogUtils.logInfo("AppSettings.Alert.Slack.Activated Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxShowHideCancelPaymentButton.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.ShowCancelPaymentButton", isChecked)
            LogUtils.logInfo("AppSettings.Options.ShowCancelPaymentButton Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxNotAllowWalletNonRfidActivated.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.NotAllowWalletNonRfid", isChecked)
            LogUtils.logInfo("AppSettings.Options.NotAllowWalletNonRfid Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxIgnoreWhenRemovingTags.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.IgnoreWhenRemovingTags", isChecked)
            LogUtils.logInfo("AppSettings.Options.IgnoreWhenRemovingTags Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxDiscountWithFormat.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.Discount.DiscountByFormat", isChecked)
            LogUtils.logInfo("AppSettings.Options.Discount.DiscountByFormat Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxSyncOrderRealtime.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxSyncOrderPeriodicPerTimePeriod.isChecked = false
                binding.checkboxSyncSpecifiedTime.isChecked = false
                binding.checkboxNoSyncOrder.isChecked = false

                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderRealtime", isChecked)
                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod", false)
                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderSpecifiedTime", false)
                PrefUtil.setBoolean("AppSettings.Options.Sync.NoSyncOrder", false)

                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderRealtime Options: $isChecked")
                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod Options: false")
                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderSpecifiedTime Options: false")
                LogUtils.logInfo("AppSettings.Options.Sync.NoSyncOrder Options: false")

                showMessageSuccess()
            }
        }

        binding.checkboxSyncOrderPeriodicPerTimePeriod.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxSyncOrderRealtime.isChecked = false
                binding.checkboxSyncSpecifiedTime.isChecked = false
                binding.checkboxNoSyncOrder.isChecked = false

                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderRealtime", false)
                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod", isChecked)
                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderSpecifiedTime", false)
                PrefUtil.setBoolean("AppSettings.Options.Sync.NoSyncOrder", false)

                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderRealtime Options: false")
                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod Options: $isChecked")
                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderSpecifiedTime Options: false")
                LogUtils.logInfo("AppSettings.Options.Sync.NoSyncOrder Options: false")

                showMessageSuccess()
            }
        }

        binding.checkboxSyncSpecifiedTime.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxSyncOrderRealtime.isChecked = false
                binding.checkboxSyncOrderPeriodicPerTimePeriod.isChecked = false
                binding.checkboxNoSyncOrder.isChecked = false

                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderRealtime", false)
                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod", false)
                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderSpecifiedTime", isChecked)
                PrefUtil.setBoolean("AppSettings.Options.Sync.NoSyncOrder", false)

                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderRealtime Options: false")
                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod Options: false")
                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderSpecifiedTime Options: $isChecked")
                LogUtils.logInfo("AppSettings.Options.Sync.NoSyncOrder Options: false")

                showMessageSuccess()
            }
        }

        binding.checkboxNoSyncOrder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxSyncOrderRealtime.isChecked = false
                binding.checkboxSyncOrderPeriodicPerTimePeriod.isChecked = false
                binding.checkboxSyncSpecifiedTime.isChecked = false

                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderRealtime", false)
                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod", false)
                PrefUtil.setBoolean("AppSettings.Options.Sync.SyncOrderSpecifiedTime", false)
                PrefUtil.setBoolean("AppSettings.Options.Sync.NoSyncOrder", isChecked)

                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderRealtime Options: false")
                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod Options: false")
                LogUtils.logInfo("AppSettings.Options.Sync.SyncOrderSpecifiedTime Options: false")
                LogUtils.logInfo("AppSettings.Options.Sync.NoSyncOrder Options: $isChecked")

                showMessageSuccess()
            }
        }

        binding.checkboxGetReaderLog.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.AllowGetReaderLog", isChecked)
            LogUtils.logInfo("AppSettings.Options.AllowGetReaderLog Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxAutoSyncMenu.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.AllowAutoSyncMenu", isChecked)
            LogUtils.logInfo("AppSettings.Options.AllowAutoSyncMenu Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxCancelPayment.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.AllowAdminCancelPayment", isChecked)
            LogUtils.logInfo("AppSettings.Options.AllowAdminCancelPayment Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxCashPaymentApproval.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.AllowAdminCashPaymentApproval", isChecked)
            LogUtils.logInfo("AppSettings.Options.AllowAdminCashPaymentApproval Options: $isChecked")
            showMessageSuccess()
        }

        binding.checkboxDiscountApproval.setOnCheckedChangeListener { _, isChecked ->
            PrefUtil.setBoolean("AppSettings.Options.AllowAdminDiscountApproval", isChecked)
            LogUtils.logInfo("AppSettings.Options.AllowAdminDiscountApproval Options: $isChecked")
            showMessageSuccess()
        }
    }

    private fun initData() {
        //====================Machine type================================
        when (AppSettings.Options.MachineTypeActivated) {
            MachineType.MAGIC_PLATE_MODE.value -> {
                binding.checkboxMagicPlateMode.isChecked = true
                binding.checkboxSelfKioskMode.isChecked = false
                binding.checkboxDiscountMode.isChecked = false
            }
            MachineType.SELF_KIOSK_MODE.value -> {
                binding.checkboxMagicPlateMode.isChecked = false
                binding.checkboxSelfKioskMode.isChecked = true
                binding.checkboxDiscountMode.isChecked = false
            }
            MachineType.DISCOUNT_MODE.value -> {
                binding.checkboxMagicPlateMode.isChecked = false
                binding.checkboxSelfKioskMode.isChecked = false
                binding.checkboxDiscount.isChecked = true
                binding.checkboxDiscountMode.isChecked = true
            }
            MachineType.POS_MODE.value -> {

            }
        }

        //====================Payment type================================
        when (AppSettings.Options.Payment.DeviceType) {
            PaymentDeviceType.IUC.value -> { // IUC
              binding.checkboxPaymentDeviceTypeIuc.isChecked = true
              binding.checkboxPaymentDeviceTypeIm30.isChecked = false
            }
            else -> { // IM30
                binding.checkboxPaymentDeviceTypeIuc.isChecked = false
                binding.checkboxPaymentDeviceTypeIm30.isChecked = true
            }
        }

        //====================Payment mode================================
        binding.checkboxMasterCard.isChecked = AppSettings.Options.Payment.MasterCard
        binding.checkboxEzlink.isChecked = AppSettings.Options.Payment.EzLink
        binding.checkboxWallet.isChecked = AppSettings.Options.Payment.Wallet
        binding.checkboxPayNow.isChecked = AppSettings.Options.Payment.PayNow
        binding.checkboxCash.isChecked = AppSettings.Options.Payment.Cash
        binding.checkboxDiscount.isChecked = binding.checkboxDiscountMode.isChecked
        binding.checkboxSelectProduct.isChecked = AppSettings.Options.Payment.SelectProduct
        binding.checkboxTopUp.isChecked = AppSettings.Options.Payment.TopUp

        if (AppSettings.Options.Payment.pathImageMasterCard.isNotEmpty()) {
            val imgFile = File(AppSettings.Options.Payment.pathImageMasterCard)
            if (imgFile.exists()) {
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imageMasterCardDefault.setImageBitmap(imgBitmap)
            }
        }
        if (AppSettings.Options.Payment.pathImageEzLink.isNotEmpty()) {
            val imgFile = File(AppSettings.Options.Payment.pathImageEzLink)
            if (imgFile.exists()) {
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imageEzLinkDefault.setImageBitmap(imgBitmap)
            }
        }
        if (AppSettings.Options.Payment.pathImagePayNow.isNotEmpty()) {
            val imgFile = File(AppSettings.Options.Payment.pathImagePayNow)
            if (imgFile.exists()) {
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imagePayNowDefault.setImageBitmap(imgBitmap)
            }
        }
        if (AppSettings.Options.Payment.pathImageWallet.isNotEmpty()) {
            val imgFile = File(AppSettings.Options.Payment.pathImageWallet)
            if (imgFile.exists()) {
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imageWalletDefault.setImageBitmap(imgBitmap)
            }
        }
        if (AppSettings.Options.Payment.pathImageCash.isNotEmpty()) {
            val imgFile = File(AppSettings.Options.Payment.pathImageCash)
            if (imgFile.exists()) {
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imageCashDefault.setImageBitmap(imgBitmap)
            }
        }
        if (AppSettings.Options.Payment.pathImageDiscount.isNotEmpty()) {
            val imgFile = File(AppSettings.Options.Payment.pathImageDiscount)
            if (imgFile.exists()) {
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imageDiscountDefault.setImageBitmap(imgBitmap)
            }
        }
        if (AppSettings.Options.Payment.pathImageSelectProduct.isNotEmpty()) {
            val imgFile = File(AppSettings.Options.Payment.pathImageSelectProduct)
            if (imgFile.exists()) {
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imageSelectProductDefault.setImageBitmap(imgBitmap)
            }
        }
        if (AppSettings.Options.Payment.pathImageTopUp.isNotEmpty()) {
            val imgFile = File(AppSettings.Options.Payment.pathImageTopUp)
            if (imgFile.exists()) {
                val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imageTopUpDefault.setImageBitmap(imgBitmap)
            }
        }

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

        binding.checkboxNfc.isChecked = AppSettings.Options.Discount.NFC
        binding.checkboxBarcode.isChecked = AppSettings.Options.Discount.Barcode

        binding.checkboxAlertTelegramActivated.isChecked = AppSettings.Alert.Telegram.Activated
        binding.checkboxAlertSlackActivated.isChecked = AppSettings.Alert.Slack.Activated

        binding.checkboxShowHideCancelPaymentButton.isChecked = AppSettings.Options.ShowCancelPaymentButton
        binding.checkboxNotAllowWalletNonRfidActivated.isChecked = AppSettings.Options.NotAllowWalletNonRfid
        binding.checkboxIgnoreWhenRemovingTags.isChecked = AppSettings.Options.IgnoreWhenRemovingTags

        binding.checkboxSyncOrderRealtime.isChecked = AppSettings.Options.Sync.SyncOrderRealtime
        binding.checkboxSyncOrderPeriodicPerTimePeriod.isChecked = AppSettings.Options.Sync.SyncOrderPeriodicPerTimePeriod
        binding.checkboxSyncSpecifiedTime.isChecked = AppSettings.Options.Sync.SyncOrderSpecifiedTime
        binding.checkboxNoSyncOrder.isChecked = AppSettings.Options.Sync.NoSyncOrder

        binding.checkboxGetReaderLog.isChecked = AppSettings.Options.AllowGetReaderLog
        binding.checkboxAutoSyncMenu.isChecked = AppSettings.Options.AllowAutoSyncMenu
        binding.checkboxCancelPayment.isChecked = AppSettings.Options.AllowAdminCancelPayment
        binding.checkboxCashPaymentApproval.isChecked = AppSettings.Options.AllowAdminCashPaymentApproval
        binding.checkboxDiscountApproval.isChecked = AppSettings.Options.AllowAdminDiscountApproval

        binding.keyCodeCancelPayment.setText(AppSettings.Options.KeyCodeCancelPayment)
        binding.keyCodeCashPaymentApproval.setText(AppSettings.Options.KeyCodeCashPaymentApproval)
        binding.keyCodeDiscountApproval.setText(AppSettings.Options.KeyCodeDiscountApproval)

        binding.checkboxDiscountWithFormat.isChecked = AppSettings.Options.Discount.DiscountByFormat
    }

    private fun showMessageSuccess() {
        AlertDialogUtil.showSuccess(getString(R.string.message_success_save), requireContext())

        // Refresh Configuration
        AppSettings.getAllSetting()
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        // GIVE AN INTEGER VALUE FOR IMAGE_PICK_CODE LIKE 1000
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            val selectedImage: Uri? = data?.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

            val cursor: Cursor? = selectedImage?.let {
                requireContext().contentResolver.query(it, filePathColumn, null, null, null)
            }
            cursor?.moveToFirst()

            val columnIndex: Int? = cursor?.getColumnIndex(filePathColumn[0])
            val picturePath: String? = columnIndex?.let { cursor.getString(it) }
            cursor?.close()

            // Get Image path
            if (picturePath != null) {
                saveOptionsPayment(picturePath)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun saveOptionsPayment(picturePath: String) {
        when (accessType) {
            PaymentModeType.MASTER_CARD.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageMasterCardDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageMasterCard", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageMasterCard : $picturePath")
            }
            PaymentModeType.EZ_LINK.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageEzLinkDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageEzLink", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageEzLink : $picturePath")
            }
            PaymentModeType.KONBINI_WALLET.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageWalletDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageWallet", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageWallet : $picturePath")
            }
            PaymentModeType.PAY_NOW.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imagePayNowDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImagePayNow", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImagePayNow : $picturePath")
            }
            PaymentModeType.CASH.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageCashDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageCash", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageCash : $picturePath")
            }
            PaymentModeType.DISCOUNT.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageDiscountDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageDiscount", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageDiscount : $picturePath")
            }
            PaymentModeType.SELECT_PRODUCT.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageSelectProductDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageSelectProduct", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageSelectProduct : $picturePath")
            }
            PaymentModeType.TOP_UP.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageTopUpDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageTopUp", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageTopUp : $picturePath")
            }
        }

        showMessageSuccess()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "KEY_CODE" -> {
                    val pressedKey: String = intent.getStringExtra("pressedKey").toString()
                    if (pressedKey.isNotEmpty()) {
                        if (binding.keyCodeCancelPayment.hasFocus()) {
                            if (pressedKey != AppSettings.Options.KeyCodeCashPaymentApproval && pressedKey != AppSettings.Options.KeyCodeDiscountApproval) {
                                PrefUtil.setString(
                                    "AppSettings.Options.KeyCodeCancelPayment",
                                    pressedKey
                                )
                                binding.keyCodeCancelPayment.setText(pressedKey)
                                // Refresh Configuration
                                AppSettings.getAllSetting()
                            } else {
                                AlertDialogUtil.showError(
                                    String.format(
                                        getString(R.string.message_error_has_been_used),
                                        pressedKey.uppercase()
                                    ),
                                    requireContext()
                                )
                            }
                        }
                        if (binding.keyCodeCashPaymentApproval.hasFocus()) {
                            if (pressedKey != AppSettings.Options.KeyCodeCancelPayment && pressedKey != AppSettings.Options.KeyCodeDiscountApproval) {
                                PrefUtil.setString(
                                    "AppSettings.Options.KeyCodeCashPaymentApproval",
                                    pressedKey
                                )
                                binding.keyCodeCashPaymentApproval.setText(pressedKey)
                                // Refresh Configuration
                                AppSettings.getAllSetting()
                            } else {
                                AlertDialogUtil.showError(
                                    String.format(
                                        getString(R.string.message_error_has_been_used),
                                        pressedKey.uppercase()
                                    ),
                                    requireContext()
                                )
                            }
                        }
                        if (binding.keyCodeDiscountApproval.hasFocus()) {
                            if (pressedKey != AppSettings.Options.KeyCodeCashPaymentApproval && pressedKey != AppSettings.Options.KeyCodeCancelPayment) {
                                PrefUtil.setString(
                                    "AppSettings.Options.KeyCodeDiscountApproval",
                                    pressedKey
                                )
                                binding.keyCodeDiscountApproval.setText(pressedKey)
                                // Refresh Configuration
                                AppSettings.getAllSetting()
                            } else {
                                AlertDialogUtil.showError(
                                    String.format(
                                        getString(R.string.message_error_has_been_used),
                                        pressedKey.uppercase()
                                    ),
                                    requireContext()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filterIntent = IntentFilter()
        filterIntent.addAction("KEY_CODE")
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(broadcastReceiver, IntentFilter(filterIntent))
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver)
        super.onStop()
    }
}