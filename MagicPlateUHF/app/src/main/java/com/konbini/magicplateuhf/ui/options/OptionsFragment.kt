package com.konbini.magicplateuhf.ui.options

import android.Manifest
import android.app.Activity
import android.content.Intent
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
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.enum.AcsReaderType
import com.konbini.magicplateuhf.data.enum.MachineType
import com.konbini.magicplateuhf.data.enum.PaymentType
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
        binding.checkboxMagicPlateMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setString(
                    "AppSettings.Options.MachineTypeActivated",
                    MachineType.MAGIC_PLATE_MODE.value
                )
                binding.checkboxMagicPlateMode.isChecked = true
                binding.checkboxSelfKioskMode.isChecked = false
                binding.checkboxDiscountMode.isChecked = false

                // Reset
                AppContainer.CurrentTransaction.resetTemporaryInfo()

                showMessageSuccess()
            }
        }

        binding.checkboxSelfKioskMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setString(
                    "AppSettings.Options.MachineTypeActivated",
                    MachineType.SELF_KIOSK_MODE.value
                )
                binding.checkboxMagicPlateMode.isChecked = false
                binding.checkboxSelfKioskMode.isChecked = true
                binding.checkboxDiscountMode.isChecked = false

                // Reset
                AppContainer.CurrentTransaction.resetTemporaryInfo()

                showMessageSuccess()
            }
        }

        binding.checkboxDiscountMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setString(
                    "AppSettings.Options.MachineTypeActivated",
                    MachineType.DISCOUNT_MODE.value
                )
                binding.checkboxMagicPlateMode.isChecked = false
                binding.checkboxSelfKioskMode.isChecked = false
                binding.checkboxDiscountMode.isChecked = true

                // Reset
                AppContainer.CurrentTransaction.resetTemporaryInfo()

                showMessageSuccess()
            }
        }

        binding.checkboxMasterCard.setOnCheckedChangeListener { buttonView, isChecked ->
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
                accessType = PaymentType.MASTER_CARD.value
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
                accessType = PaymentType.MASTER_CARD.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxEzlink.setOnCheckedChangeListener { buttonView, isChecked ->
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
                accessType = PaymentType.EZ_LINK.value
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
                accessType = PaymentType.EZ_LINK.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxWallet.setOnCheckedChangeListener { buttonView, isChecked ->
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
                accessType = PaymentType.KONBINI_WALLET.value
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
                accessType = PaymentType.KONBINI_WALLET.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxPayNow.setOnCheckedChangeListener { buttonView, isChecked ->
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
                accessType = PaymentType.PAY_NOW.value
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
                accessType = PaymentType.PAY_NOW.value
                saveOptionsPayment("")
            }
        }

        binding.checkboxCash.setOnCheckedChangeListener { buttonView, isChecked ->
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
                accessType = PaymentType.CASH.value
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
                accessType = PaymentType.CASH.value
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
                accessType = PaymentType.DISCOUNT.value
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
                accessType = PaymentType.DISCOUNT.value
                saveOptionsPayment("")
            }
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

        binding.checkboxNfc.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Discount.NFC", true)
                PrefUtil.setBoolean("AppSettings.Options.Discount.Barcode", false)
                binding.checkboxNfc.isChecked = true
                binding.checkboxBarcode.isChecked = false
                showMessageSuccess()
            }
        }

        binding.checkboxBarcode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                PrefUtil.setBoolean("AppSettings.Options.Discount.NFC", false)
                PrefUtil.setBoolean("AppSettings.Options.Discount.Barcode", true)
                binding.checkboxNfc.isChecked = false
                binding.checkboxBarcode.isChecked = true
                showMessageSuccess()
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
                binding.checkboxDiscountMode.isChecked = true
            }
            MachineType.POS_MODE.value -> {

            }
        }

        binding.checkboxMasterCard.isChecked = AppSettings.Options.Payment.MasterCard
        binding.checkboxEzlink.isChecked = AppSettings.Options.Payment.EzLink
        binding.checkboxWallet.isChecked = AppSettings.Options.Payment.Wallet
        binding.checkboxPayNow.isChecked = AppSettings.Options.Payment.PayNow
        binding.checkboxCash.isChecked = AppSettings.Options.Payment.Cash

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
            PaymentType.MASTER_CARD.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageMasterCardDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageMasterCard", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageMasterCard : $picturePath")
            }
            PaymentType.EZ_LINK.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageEzLinkDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageEzLink", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageEzLink : $picturePath")
            }
            PaymentType.KONBINI_WALLET.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageWalletDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageWallet", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageWallet : $picturePath")
            }
            PaymentType.PAY_NOW.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imagePayNowDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImagePayNow", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImagePayNow : $picturePath")
            }
            PaymentType.CASH.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageCashDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageCash", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageCash : $picturePath")
            }
            PaymentType.DISCOUNT.value -> {
                val imgFile = File(picturePath)
                if (imgFile.exists()) {
                    val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.imageDiscountDefault.setImageBitmap(imgBitmap)
                }
                PrefUtil.setString("AppSettings.Options.Payment.pathImageDiscount", picturePath)
                LogUtils.logInfo("AppSettings.Options.Payment.pathImageDiscount : $picturePath")
            }
        }

        showMessageSuccess()
    }
}