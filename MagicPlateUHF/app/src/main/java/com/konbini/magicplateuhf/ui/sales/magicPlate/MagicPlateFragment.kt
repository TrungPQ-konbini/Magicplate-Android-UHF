package com.konbini.magicplateuhf.ui.sales.magicPlate

import android.app.PendingIntent
import android.content.*
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.acs.smartcard.Reader
import com.acs.smartcard.ReaderException
import com.aigestudio.wheelpicker.WheelPicker
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.developer.kalert.KAlertDialog
import com.google.gson.Gson
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.*
import com.konbini.magicplateuhf.data.entities.*
import com.konbini.magicplateuhf.data.enum.*
import com.konbini.magicplateuhf.databinding.FragmentMagicPlateBinding
import com.konbini.magicplateuhf.hardware.IM30Interface
import com.konbini.magicplateuhf.ui.sales.magicPlate.adapters.CartAdapter
import com.konbini.magicplateuhf.ui.sales.magicPlate.adapters.CategoryAdapter
import com.konbini.magicplateuhf.ui.sales.magicPlate.adapters.PaymentAdapter
import com.konbini.magicplateuhf.ui.sales.magicPlate.adapters.ProductAdapter
import com.konbini.magicplateuhf.ui.sales.magicPlate.dialogs.ModifiersDialog
import com.konbini.magicplateuhf.ui.settings.SettingsViewModel
import com.konbini.magicplateuhf.utils.*
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.blink
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.convertStringToShortTime
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.formatCurrency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.math.roundToInt


@AndroidEntryPoint
class MagicPlateFragment : Fragment(),
    PaymentAdapter.ItemListener,
    CartAdapter.ItemListener,
    CategoryAdapter.ItemListener,
    ProductAdapter.ItemListener,
    WheelPicker.OnItemSelectedListener {

    companion object {
        const val TAG = "MagicPlateFragment"
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        var displayName = ""
        var balance = 0F

        val ALARM_DELAY = AppSettings.Timer.DelayAlert
    }

    private var orderNumber = 0
    private val gson = Gson()
    private var contentReceipt = ""

    private var timeAlarm = 0L

    // Variable for Discount
    private var barcode: String = ""
    private var defaultInterval: Int = 1000
    private var lastTimeClicked: Long = 0
    private lateinit var pDialog: KAlertDialog

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "REFRESH_READER_TAGS" -> {
                    // Add to Mask real-time reading tags
                    var content = String.format(
                        getString(R.string.title_count_n_tags),
                        AppContainer.GlobalVariable.listEPC.size
                    )
                    if (AppContainer.GlobalVariable.listEPC.size > 0) {
                        content += "\n==============================\n"
                        val listTagEntity =
                            AppContainer.GlobalVariable.getListTagEntity(AppContainer.GlobalVariable.listEPC)
                        listTagEntity.forEach { tagEntity ->
                            content += "${tagEntity.strEPC} | ${tagEntity.plateModelTitle} \n"
                        }
                    }
                    if (binding.rfidMaskReading.isVisible)
                        binding.rfidRealTimeTags.text = content
                }
                "REFRESH_TAGS" -> {
                    when (AppContainer.CurrentTransaction.paymentState) {
                        /**
                         * TODO: The reason we had ALARM last time was because people press Payment then walk away.
                         *       Can we have the Alarm, but make it ring only if we detect all tags/plates removed while we are still in PAYMENT mode.
                         */
                        PaymentState.ReadyToPay,
                        PaymentState.InProgress -> {
                            val current = System.currentTimeMillis()
                            // Check Cart Locked Change
                            when (AppContainer.CurrentTransaction.checkCartHasBarcodeOrTags()) {
                                // cart has only tags
                                0 -> {
                                    if (AppContainer.CurrentTransaction.cart.isEmpty()) {
                                        val offset = current - timeAlarm
                                        if (offset > ALARM_DELAY) {
                                            MainApplication.mAudioManager.soundBuzzer()
                                            setBlink(AlarmType.ERROR)
                                        }
                                        return
                                    } else {
                                        timeAlarm = 0L
                                    }
                                }
                                // cart has only barcodes
                                1 -> {
                                    // do nothing
                                }
                                // cart with tags and barcodes
                                else -> {
                                    val cartWithTag =
                                        AppContainer.CurrentTransaction.cart.filter { cartEntity ->
                                            cartEntity.strEPC.isNotEmpty()
                                        }
                                    if (cartWithTag.isEmpty()) {
                                        val offset = current - timeAlarm
                                        if (offset > ALARM_DELAY) {
                                            MainApplication.mAudioManager.soundBuzzer()
                                            setBlink(AlarmType.ERROR)
                                        }
                                        return
                                    } else {
                                        timeAlarm = 0L
                                    }
                                }
                            }
                        }
                        PaymentState.Init,
                        PaymentState.Preparing -> {
                            // Refresh cart
                            refreshCart()
                        }
                        else -> {

                        }
                    }
                }
                "NEW_BARCODE" -> {
                    val paymentState = AppContainer.CurrentTransaction.paymentState
                    if (paymentState == PaymentState.InProgress || paymentState == PaymentState.ReadyToPay) {
                        MainApplication.mAudioManager.soundDoNotChangeItem()
                        return
                    }
                    val barcode = AppContainer.CurrentTransaction.barcode.split("\n")[0]
                    if (barcode.isEmpty() || barcode.length == 1) return
                    val product =
                        AppContainer.GlobalVariable.listProducts.find { _productEntity -> _productEntity.barcode == barcode }
                    if (product != null) {
                        val cartEntity = CartEntity(
                            uuid = UUID.randomUUID().toString(),
                            strEPC = "",
                            menuDate = "",
                            timeBlockId = "",
                            productId = product.id.toString(),
                            plateModelId = "",
                            price = product.regularPrice,
                            productName = product.name,
                            plateModelName = "",
                            plateModelCode = "",
                            timeBlockTitle = "",
                            quantity = 1,
                            options = product.options
                        )
                        AppContainer.CurrentTransaction.cart.add(cartEntity)
                        // Refresh cart
                        AppContainer.CurrentTransaction.refreshCart()
                        refreshCart()
                    } else {
                        AlertDialogUtil.showError(
                            getString(R.string.message_error_product_not_found),
                            requireContext()
                        )
                    }
                }
                "ACCEPT_OPTIONS" -> {
                    // Refresh cart
                    AppContainer.CurrentTransaction.refreshCart()
                    refreshCart()
                }
                "MQTT_SYNC_DATA" -> {
                    viewModelSettings.syncAll()
                    Timer()
                }
                "KEY_CODE" -> {
                    if (AppSettings.Options.AllowAdminCancelPayment) {
                        val pressedKey: String = intent.getStringExtra("pressedKey").toString()
                        LogUtils.logInfo("User pressed key | $pressedKey")
                        if (pressedKey.isNotEmpty()) {
                            adminCancelPayment(pressedKey)
                        }
                    } else {
                        AlertDialogUtil.showError(
                            getString(R.string.message_allow_admin_cancel_payment_unchecked),
                            requireContext()
                        )
                    }

                    if (AppContainer.CurrentTransaction.paymentModeType == PaymentModeType.CASH) {
                        // Check Admin cash payment approval
                        if (AppSettings.Options.AllowAdminCashPaymentApproval) {
                            val pressedKey: String = intent.getStringExtra("pressedKey").toString()
                            LogUtils.logInfo("User pressed key | $pressedKey")
                            if (pressedKey.isNotEmpty()) {
                                adminCashPaymentApproval(pressedKey)
                            }
                        } else {
                            AlertDialogUtil.showError(
                                getString(R.string.message_allow_admin_cash_payment_approval_unchecked),
                                requireContext()
                            )
                        }
                    }

                    if (AppSettings.Options.AllowAdminDiscountApproval) {
                        val pressedKey: String = intent.getStringExtra("pressedKey").toString()
                        LogUtils.logInfo("User pressed key | $pressedKey")
                        if (pressedKey.isNotEmpty()) {
                            adminDiscountApproval(pressedKey)
                        }
                    } else {
                        AlertDialogUtil.showError(
                            getString(R.string.message_allow_admin_discount_approved_unchecked),
                            requireContext()
                        )
                    }
                }
            }
        }
    }

    private var timeout = AppSettings.Options.Payment.Timeout
    private val timerTimeoutPayment = object : CountDownTimer(timeout * 1000, 1000) {
        override fun onTick(p0: Long) {
            timeout -= 1
            if (timeout > 0) {
                val message =
                    String.format(getString(R.string.message_please_tap_card_in_seconds), timeout)
                displayMessage(message, true)
            }
        }

        override fun onFinish() {
            cancelPayment(isTimeout = true)
        }

    }

    private val timerTimeoutDiscount = object : CountDownTimer(timeout * 1000, 1000) {
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            // Hide icon loading
            hideDialogDiscount()
            cancel()
        }

    }

    private var clickedTitleHeaderIndex = 0
    private var clickedTitleModel = 0

    private lateinit var cartAdapter: CartAdapter
    private lateinit var paymentAdapter: PaymentAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var productAdapter: ProductAdapter
    private var listPaymentType: MutableList<String> = mutableListOf()
    private var listShortcut: MutableList<Double> = mutableListOf()

    private var binding: FragmentMagicPlateBinding by autoCleared()
    private val viewModel: MagicPlateViewModel by viewModels()
    private val viewModelSettings: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMagicPlateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWheelPicker()
        setupRecyclerView()
        setupObservers()
        setupActions()
        listenerAcsReader()
    }

    override fun onStart() {
        super.onStart()
        val filterIntent = IntentFilter()
        filterIntent.addAction("NEW_BARCODE")
        filterIntent.addAction("REFRESH_TAGS")
        filterIntent.addAction("REFRESH_READER_TAGS")
        filterIntent.addAction("ACCEPT_OPTIONS")
        filterIntent.addAction("MQTT_SYNC_DATA")
        filterIntent.addAction("KEY_CODE")
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(broadcastReceiver, IntentFilter(filterIntent))
    }

    override fun onResume() {
        super.onResume()
        initData()
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver)
        super.onStop()
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    /**
     * Setup WheelPicker
     *
     */
    private fun setupWheelPicker() {
        listShortcut = AppSettings.Shortcut.Amount.split(",").map { it.toDouble() }.toMutableList()
        val data = mutableListOf<String>()
        listShortcut.forEach { shortcut ->
            data.add(formatCurrency(shortcut.toFloat()))
        }
        binding.wheelShortcutsTopUp.data = data
        binding.wheelShortcutsTopUp.setOnItemSelectedListener(this)
    }

    /**
     * Setup recycler view
     *
     */
    private fun setupRecyclerView() {
        initRecyclerViewPayments()
        initRecyclerViewCart()
        initRecyclerViewCategories()
        initRecyclerViewProducts()
    }

    /**
     * Init recycler view payments
     *
     */
    private fun initRecyclerViewPayments(checkNonRFID: Boolean = false) {
        listPaymentType.clear()
        if (AppSettings.Options.Payment.MasterCard) {
            listPaymentType.add(PaymentModeType.MASTER_CARD.value)
        }
        if (AppSettings.Options.Payment.EzLink) {
            listPaymentType.add(PaymentModeType.EZ_LINK.value)
        }
        if (AppSettings.Options.Payment.PayNow) {
            listPaymentType.add(PaymentModeType.PAY_NOW.value)
        }
        if (checkNonRFID) {
            val cartHasNonRFID = AppContainer.CurrentTransaction.checkCartHasNonRFID()
            if (!cartHasNonRFID) {
                // Show wallet payment mode
                if (AppSettings.Options.Payment.Wallet) {
                    listPaymentType.add(PaymentModeType.KONBINI_WALLET.value)
                }
            }
        } else {
            if (AppSettings.Options.Payment.Wallet) {
                listPaymentType.add(PaymentModeType.KONBINI_WALLET.value)
            }
        }
        if (AppSettings.Options.Payment.Cash) {
            listPaymentType.add(PaymentModeType.CASH.value)
        }
        if (AppSettings.Options.MachineTypeActivated == MachineType.DISCOUNT_MODE.value) {
            listPaymentType.add(PaymentModeType.DISCOUNT.value)
        }
        if (AppSettings.Options.Payment.SelectProduct) {
            listPaymentType.add(PaymentModeType.SELECT_PRODUCT.value)
        }
        if (AppSettings.Options.Payment.TopUp) {
            listPaymentType.add(PaymentModeType.TOP_UP.value)
        }

        if (!checkNonRFID) {
            var spanCount = listPaymentType.size / 2
            if (listPaymentType.size % 2 > 0) spanCount++
            paymentAdapter = PaymentAdapter(this)
            val manager =
                GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false)
            binding.recyclerViewPayments.layoutManager = manager
            binding.recyclerViewPayments.adapter = paymentAdapter
            paymentAdapter.setItems(items = ArrayList(listPaymentType))
        } else {
            if (paymentAdapter.itemCount != listPaymentType.size)
                paymentAdapter.setItems(items = ArrayList(listPaymentType))
        }
    }

    /**
     * Init recycler view cart
     *
     */
    private fun initRecyclerViewCart() {
        cartAdapter = CartAdapter(requireContext(), this)
        val manager = LinearLayoutManager(requireContext())
        binding.rfidProducts.layoutManager = manager
        binding.rfidProducts.adapter = cartAdapter

        val mDividerItemDecoration = DividerItemDecoration(
            binding.rfidProducts.context,
            LinearLayoutManager.VERTICAL
        )
        binding.rfidProducts.addItemDecoration(mDividerItemDecoration)
    }

    /**
     * Init recycler view categories
     *
     */
    private fun initRecyclerViewCategories() {
        categoryAdapter = CategoryAdapter(this)
        val manager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewCategories.layoutManager = manager
        binding.recyclerViewCategories.adapter = categoryAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            val listCategories = viewModel.getAllCategories().toMutableList()
            val category = CategoryEntity(
                id = 0,
                name = "All",
                parent = 0,
                menuOrder = 0
            )
            listCategories.add(category)
            listCategories.sortBy { categoryEntity -> categoryEntity.id }
            categoryAdapter.setItems(items = ArrayList(listCategories))
        }
    }

    /**
     * Init recycler view categories
     *
     */
    private fun initRecyclerViewProducts() {
        val spanCount = 5
        productAdapter = ProductAdapter(this)
        val manager =
            GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false)
        binding.recyclerViewProducts.layoutManager = manager
        binding.recyclerViewProducts.adapter = productAdapter

        if (AppContainer.GlobalVariable.listProducts.isEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                val listProducts = viewModel.getAllProducts().toMutableList()
                AppContainer.GlobalVariable.listProducts = listProducts
                productAdapter.setItems(items = ArrayList(listProducts))
            }
        } else {
            productAdapter.setItems(items = ArrayList(AppContainer.GlobalVariable.listProducts))
        }
    }

    /**
     * Setup observers
     *
     */
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModelSettings.state.collect() { _state ->
                when (_state.status) {
                    Resource.Status.LOADING -> {
                        binding.spinTitle.text = getString(R.string.message_syncing)
                        showHideLoading(true)
                    }
                    Resource.Status.SUCCESS -> {
                        // Init data
                        initData(startLoading = false)
                        AlertDialogUtil.showSuccess(_state.message, requireContext())
                    }
                    Resource.Status.ERROR -> {
                        showHideLoading(false)
                        AlertDialogUtil.showError(_state.message, requireContext())
                    }
                    else -> {
                        showHideLoading(false)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.state.collect { _state ->
                    when (_state.status) {
                        Resource.Status.LOADING -> {
                            // Reset countdown timeout payment
                            timerTimeoutPayment.cancel()
                            timeout = AppSettings.Options.Payment.Timeout

                            AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress

                            displayMessage(_state.message)
                            LogUtils.logInfo(_state.message)
                        }
                        Resource.Status.SUCCESS -> {
                            LogUtils.logInfo("Start Blink Green")
                            setBlink(AlarmType.SUCCESS)
                            displayMessage(_state.message)
                            MainApplication.mAudioManager.soundPaymentSuccess()
                            LogUtils.logInfo(_state.message)
                            AppContainer.CurrentTransaction.paymentState = PaymentState.Success

                            if (AppSettings.Options.Printer.Bluetooth || AppSettings.Options.Printer.USB) {
                                // Print Receipt
                                LogUtils.logInfo("Start Print receipt")
                                val cartLocked: MutableList<CartEntity> =
                                    ArrayList(AppContainer.CurrentTransaction.cartLocked)
                                printReceipt(cartLocked,AppContainer.CurrentTransaction.currentDiscount)
                            }

                            Log.e("EKRON", "PaymentState.Success")
                            AppContainer.CurrentTransaction.resetTemporaryInfo()
                            // Refresh cart
                            refreshCart()

                            val message = getString(R.string.message_put_plate_on_the_tray)
                            resetMessage(message, 0)
                        }
                        Resource.Status.ERROR -> {
                            setBlink(AlarmType.ERROR)
                            displayMessage(_state.message)
                            MainApplication.mAudioManager.soundBuzzer()
                            AppContainer.CurrentTransaction.paymentState = PaymentState.Error

                            val message = getString(R.string.message_put_plate_on_the_tray)
                            resetMessage(message, 0)
                            AppContainer.CurrentTransaction.paymentState = PaymentState.Preparing
                        }
                        else -> {
                        }
                    }
                }
            }

            launch {
                viewModel.stateTopUp.collect { _stateTopUp ->
                    when (_stateTopUp.status) {
                        Resource.Status.LOADING -> {
                            // Reset countdown timeout payment
                            timerTimeoutPayment.cancel()
                            timeout = AppSettings.Options.Payment.Timeout

                            AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress

                            displayMessage(_stateTopUp.message)
                            LogUtils.logInfo(_stateTopUp.message)
                        }
                        Resource.Status.SUCCESS -> {
                            handleTopUpSuccess(_stateTopUp.message)
//                            LogUtils.logInfo("Start Blink Green")
//                            setBlink(AlarmType.SUCCESS)
//                            displayMessage(_stateTopUp.message)
//                            MainApplication.mAudioManager.soundPaymentSuccess()
//                            LogUtils.logInfo(_stateTopUp.message)
//                            AppContainer.CurrentTransaction.paymentState = PaymentState.Success
//
//                            if (AppSettings.Options.Printer.Bluetooth || AppSettings.Options.Printer.USB) {
//                                // Print Receipt
//                                LogUtils.logInfo("Start Print receipt top up")
//                                printTopUpReceipt()
//                            }
//
//                            AppContainer.CurrentTransaction.resetTemporaryInfo()
//
//                            val message = getString(R.string.message_put_plate_on_the_tray)
//                            resetMessage(message, 0)
                        }
                        Resource.Status.ERROR -> {
                            handleTopUpError(_stateTopUp.message)
//                            setBlink(AlarmType.ERROR)
//                            displayMessage(_stateTopUp.message)
//                            MainApplication.mAudioManager.soundBuzzer()
//                            AppContainer.CurrentTransaction.paymentState = PaymentState.Error
//
//                            val message = getString(R.string.message_put_plate_on_the_tray)
//                            resetMessage(message, 0)
//                            AppContainer.CurrentTransaction.paymentState = PaymentState.Preparing
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }

    /**
     * Setup actions
     *
     */
    private fun setupActions() {
        binding.titleHeaderIndex.setOnClickListener {
            clickedTitleHeaderIndex += 1
            if (clickedTitleHeaderIndex == 3) {
                clickedTitleHeaderIndex = 0
                binding.rfidMaskReading.isVisible = !binding.rfidMaskReading.isVisible
            }
        }

        binding.rfidItemCount.setOnClickListener {
            binding.rfidItemCount.blink(Color.RED, 1, 50L)
            clickedTitleModel += 1
            Log.e(TAG, "Clicked Title Model-$clickedTitleModel")

            if (clickedTitleModel >= 5) {
                clickedTitleModel = 0
                // Goto Login
                gotoLogin()
            }
        }

        binding.rfidCancelPayment.setSafeOnClickListener {
            cancelPayment()
        }

        binding.rfidEmptyCart.setSafeOnClickListener {
            if (AppContainer.CurrentTransaction.paymentState == PaymentState.Init ||
                AppContainer.CurrentTransaction.paymentState == PaymentState.Preparing
            ) {
                // Reset temporary info
                AppContainer.CurrentTransaction.resetTemporaryInfo()
                AppContainer.CurrentTransaction.paymentState = PaymentState.Init

                // Refresh cart
                refreshCart()
            }
        }

        // TODO: Start TrungPQ add to test
//        binding.rfidTotalCount.setSafeOnClickListener {
//            setBlink(AlarmType.SUCCESS)
//        }
//
//        binding.spinKitMessage.setSafeOnClickListener {
//            AppContainer.CurrentTransaction.cardNFC = "8d2ed739"
//            viewModel.debit()
//        }
//
//        binding.rfidMessageTitle.setSafeOnClickListener {
//            if (AppContainer.CurrentTransaction.paymentState == PaymentState.Success) {
//                AppContainer.CurrentTransaction.paymentState = PaymentState.Init
//            } else {
//                AppContainer.CurrentTransaction.barcode = "8885000035380"
//                Log.e("BARCODE_VALUE", AppContainer.CurrentTransaction.barcode)
//                val intent = Intent()
//                intent.action = "NEW_BARCODE"
//                LocalBroadcastManager.getInstance(MainApplication.instance.applicationContext)
//                    .sendBroadcast(intent)
//                barcode = ""
//            }
//            val timer = object: CountDownTimer(500, 100) {
//                override fun onTick(millisUntilFinished: Long) {
//                    // do something
//                }
//                override fun onFinish() {
//                    AppContainer.GlobalVariable.listEPC.clear()
//                    AppContainer.GlobalVariable.listEPC.add("11800000020300108C002F3F")
//
//                    AppContainer.CurrentTransaction.listEPC.clear()
//                    AppContainer.CurrentTransaction.listEPC.addAll(AppContainer.GlobalVariable.listEPC)
//
//                    // Get list tags
//                    val listTagEntity =
//                        AppContainer.GlobalVariable.getListTagEntity(AppContainer.CurrentTransaction.listEPC)
//                    AppContainer.CurrentTransaction.listTagEntity = listTagEntity
//
//                    MainApplication.timeTagSizeChanged = 0L
//                    AppContainer.CurrentTransaction.refreshCart()
//
//                    // Add or Remove items to cart
//                    val intent = Intent()
//                    intent.action = "REFRESH_TAGS"
//                    LocalBroadcastManager.getInstance(MainApplication.instance.applicationContext).sendBroadcast(intent)
//                    start()
//                }
//            }
//            timer.start()
//        }
        binding.rfidMessageTitle.setSafeOnClickListener {
//            if (AppContainer.CurrentTransaction.paymentState == PaymentState.Success) {
//                AppContainer.CurrentTransaction.paymentState = PaymentState.Init
//            } else {
//                AppContainer.CurrentTransaction.barcode = "8885000035380"
//                val intent = Intent()
//                intent.action = "NEW_BARCODE"
//                LocalBroadcastManager.getInstance(MainApplication.instance.applicationContext)
//                    .sendBroadcast(intent)
//            }

            AppContainer.CurrentTransaction.paymentModeType = PaymentModeType.TOP_UP
            viewModel.credit("")
        }
        // TODO: End TrungPQ add to test
    }

    /**
     * Init data
     *
     */
    private fun initData(startLoading: Boolean = true) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (startLoading)
                showHideLoading(true)

            AppContainer.CurrentTransaction.resetTemporaryInfo()

            AppContainer.GlobalVariable.listUsers = viewModel.getAllUsers().toMutableList()
            AppContainer.GlobalVariable.listProducts = viewModel.getAllProducts().toMutableList()
            AppContainer.GlobalVariable.listTimeBlocks = viewModel.getAllTimeBlock().toMutableList()
            AppContainer.GlobalVariable.listMenus = viewModel.getAllMenu().toMutableList()
            AppContainer.GlobalVariable.currentTimeBock = viewModel.getCurrentTimeBock()
            AppContainer.GlobalVariable.listMenusToday = viewModel.getMenusToday()

            displayTimeBlock()
            displayTotal()
            displayCountItem()
            displayMessage(getString(R.string.message_put_plate_on_the_tray))

            showHideLoading(false)

            when (AppContainer.GlobalVariable.actionRestartShutdown) {
                "RESTART" -> {
                    CommonUtil.restartOrTurnOffApplication(0)
                }
                "SHUTDOWN" -> {
                    CommonUtil.restartOrTurnOffApplication(1)
                }
            }
        }
    }

    override fun onClickedPayment(payment: String) {
        // Check selected payment
        val paymentState = AppContainer.CurrentTransaction.paymentState
        LogUtils.logInfo("User clicked payment. paymentState is | $paymentState")
        if (paymentState == PaymentState.ReadyToPay || paymentState == PaymentState.InProgress) {
            LogUtils.logInfo("User clicked payment but paymentState is | $paymentState => Ignore")
            return
        }
        when (payment) {
            PaymentModeType.MASTER_CARD.value -> {
                handleClickedMasterCardPayment()
            }
            PaymentModeType.EZ_LINK.value -> {
                handleClickedEzLinkPayment()
            }
            PaymentModeType.PAY_NOW.value -> {
                handleClickedPayNowPayment()
            }
            PaymentModeType.KONBINI_WALLET.value -> {
                handleClickedWalletPayment()
            }
            PaymentModeType.CASH.value -> {
                if (AppContainer.CurrentTransaction.isTopUp) {
                    handleClickedCashPaymentWithTopUp()
                } else {
                    handleClickedCashPayment()
                }
            }
            PaymentModeType.DISCOUNT.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                val paymentState = AppContainer.CurrentTransaction.paymentState
                if (paymentState != PaymentState.Init && paymentState != PaymentState.Preparing) {
                    return
                }

                // Check click button many times
                if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
                    return
                }

                handleClickedSelectProductButton(true)
                handleClickedDiscountButton()
            }
            PaymentModeType.SELECT_PRODUCT.value -> {
                handleClickedSelectProductButton()
            }
            PaymentModeType.TOP_UP.value -> {
                handleClickedTopUpButton()
            }
        }
    }

    override fun onClickedCartItem(cartEntity: CartEntity, type: ActionCart) {
        val state = AppContainer.CurrentTransaction.paymentState
        if (state == PaymentState.ReadyToPay || state == PaymentState.InProgress) {
            setBlink(AlarmType.ERROR)
            MainApplication.mAudioManager.soundDoNotChangeItem()
            return
        }
        when (type) {
            ActionCart.Minus -> {
                AppContainer.CurrentTransaction.cart.forEach currentCart@{ _cartEntity ->
                    if (_cartEntity.uuid == cartEntity.uuid) {
                        _cartEntity.quantity -= 1
                        AppContainer.CurrentTransaction.cart =
                            AppContainer.CurrentTransaction.cart.filter { _cartEntityItem -> _cartEntityItem.quantity != 0 }
                                .toMutableList()
                        return@currentCart
                    }
                }
                // Refresh cart
                AppContainer.CurrentTransaction.refreshCart()
                refreshCart()
            }
            ActionCart.Plus -> {
                AppContainer.CurrentTransaction.cart.forEach currentCart@{ _cartEntity ->
                    if (_cartEntity.uuid == cartEntity.uuid) {
                        _cartEntity.quantity += 1
                        return@currentCart
                    }
                }
                // Refresh cart
                AppContainer.CurrentTransaction.refreshCart()
                refreshCart()
            }
            ActionCart.Delete -> {
                AppContainer.CurrentTransaction.cart.forEach currentCart@{ _cartEntity ->
                    if (_cartEntity.uuid == cartEntity.uuid) {
                        AppContainer.CurrentTransaction.cart =
                            AppContainer.CurrentTransaction.cart.filter { _cartEntityItem -> _cartEntityItem.uuid != cartEntity.uuid }
                                .toMutableList()
                        return@currentCart
                    }
                }
                // Refresh cart
                AppContainer.CurrentTransaction.refreshCart()
                refreshCart()
            }
            ActionCart.Modifier -> {
                val dialog = ModifiersDialog(cartEntity, MachineType.MAGIC_PLATE_MODE.value)
                activity?.supportFragmentManager?.let { fragmentManager ->
                    dialog.show(
                        fragmentManager,
                        "OptionsDialog"
                    )
                }
            }
        }
    }

    override fun onClickedCategory(category: CategoryEntity) {
        AppContainer.CurrentTransaction.selectedCategory = category.id
        categoryAdapter.notifyDataSetChanged()

        // Filter products by category
        val productsFilter = AppContainer.GlobalVariable.listProducts.filter { productEntity ->
            productEntity.categories.isNotEmpty() &&
                    productEntity.categories.split(",").map { it.trim() }
                        .contains(category.id.toString())
        }

        productAdapter.setItems(items = ArrayList(productsFilter))
    }

    override fun onClickedProduct(product: ProductEntity) {
        val cartEntity = CartEntity(
            uuid = UUID.randomUUID().toString(),
            strEPC = "",
            menuDate = "",
            timeBlockId = "",
            productId = product.id.toString(),
            plateModelId = "",
            price = product.regularPrice,
            productName = product.name,
            plateModelName = "",
            plateModelCode = "",
            timeBlockTitle = "",
            quantity = 1,
            options = product.options
        )
        AppContainer.CurrentTransaction.cart.add(cartEntity)
        // Refresh cart
        AppContainer.CurrentTransaction.refreshCart()
        refreshCart()
    }

    private fun handleClickedTopUpButton(isProcessingPayment: Boolean = false) {
        if (AppContainer.CurrentTransaction.cart.isNotEmpty()) return

        var layoutWeightPayment = 4f
        if (!binding.layoutTopUp.isVisible) {
            layoutWeightPayment = 8f
            AppContainer.CurrentTransaction.isTopUp = true

            // Hide Wallet Payment, Discount, Select Product
            listPaymentType.clear()
            if (AppSettings.Options.Payment.MasterCard) {
                listPaymentType.add(PaymentModeType.MASTER_CARD.value)
            }
            if (AppSettings.Options.Payment.EzLink) {
                listPaymentType.add(PaymentModeType.EZ_LINK.value)
            }
            if (AppSettings.Options.Payment.PayNow) {
                listPaymentType.add(PaymentModeType.PAY_NOW.value)
            }
            if (AppSettings.Options.Payment.Cash) {
                listPaymentType.add(PaymentModeType.CASH.value)
            }
            if (AppSettings.Options.Payment.TopUp) {
                listPaymentType.add(PaymentModeType.TOP_UP.value)
            }
            var spanCount = listPaymentType.size / 2
            if (listPaymentType.size % 2 > 0) spanCount++
            val manager =
                GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false)
            binding.recyclerViewPayments.layoutManager = manager
            binding.recyclerViewPayments.adapter = paymentAdapter
            paymentAdapter.setItems(items = ArrayList(listPaymentType))
        } else {
            if (!isProcessingPayment)
                AppContainer.CurrentTransaction.isTopUp = false
            initRecyclerViewPayments()
        }

        // Set Layout Weight
        binding.selectPayment.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
            layoutWeightPayment
        )

        // Set show/hide
        binding.layoutTopUp.isVisible = !binding.layoutTopUp.isVisible
        binding.layoutCountAndTotal.isVisible = !binding.layoutCountAndTotal.isVisible
        binding.layoutMessageTitle.isVisible = !binding.layoutMessageTitle.isVisible

        // Clear text
        binding.textInputEditTextTopUpCcw.setText("")
        binding.textInputEditTextTopUpAmount.setText("")
    }

    override fun onItemSelected(picker: WheelPicker, data: Any, position: Int) {
        when (picker.id) {
            binding.wheelShortcutsTopUp.id -> {
                binding.textInputEditTextTopUpAmount.setText(formatCurrency(listShortcut[position].toFloat()))
            }
            else -> {
            }
        }
    }

    private fun handleClickedMasterCardPayment() {
        val validate = validateSelectPayment()
        if (!validate) return

        // Check click button many times
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            LogUtils.logInfo("User clicked ${PaymentModeType.MASTER_CARD.value} too fast")
            return
        }

        handleClickedSelectProductButton(true)

        lastTimeClicked = SystemClock.elapsedRealtime()
        LogUtils.logInfo("User clicked ${PaymentModeType.MASTER_CARD.value}")

        changeColorWhenSelectPayment()

        // Start countdown timeout and voice
        timerTimeoutPayment.start()
        MainApplication.mAudioManager.soundPleaseTapCard()

        // Change Payment state
        AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
        AppContainer.CurrentTransaction.paymentModeType = PaymentModeType.MASTER_CARD

        // Locked cart
        AppContainer.CurrentTransaction.cartLocked()

        // Listener MasterCard
        listenerMasterCard()
    }

    private fun handleClickedEzLinkPayment() {
        val validate = validateSelectPayment()
        if (!validate) return

        // Check click button many times
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }

        handleClickedSelectProductButton(true)

        lastTimeClicked = SystemClock.elapsedRealtime()
        LogUtils.logInfo("User clicked ${PaymentModeType.EZ_LINK.value}")

        changeColorWhenSelectPayment()

        // Start countdown timeout and voice
        timerTimeoutPayment.start()
        MainApplication.mAudioManager.soundPleaseTapCard()

        // Change Payment state
        AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
        AppContainer.CurrentTransaction.paymentModeType = PaymentModeType.EZ_LINK

        // Locked cart
        AppContainer.CurrentTransaction.cartLocked()

        // Listener EzLink
        listenerEzLink()
    }

    private fun handleClickedPayNowPayment() {
        val validate = validateSelectPayment()
        if (!validate) return

        // Check click button many times
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }

        handleClickedSelectProductButton(true)

        lastTimeClicked = SystemClock.elapsedRealtime()
        LogUtils.logInfo("User clicked ${PaymentModeType.PAY_NOW.value}")

        changeColorWhenSelectPayment()

        // Start countdown timeout and voice
        timerTimeoutPayment.start()
        MainApplication.mAudioManager.soundPleaseTapCard()

        // Change Payment state
        AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
        AppContainer.CurrentTransaction.paymentModeType = PaymentModeType.PAY_NOW

        // Locked cart
        AppContainer.CurrentTransaction.cartLocked()

        // Listener PayNow
        listenerPayNow()
    }

    private fun handleClickedWalletPayment() {
        val validate = validateSelectPayment()
        if (!validate) return

        // Check click button many times
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }

        handleClickedSelectProductButton(true)

        lastTimeClicked = SystemClock.elapsedRealtime()
        LogUtils.logInfo("User clicked ${PaymentModeType.KONBINI_WALLET.value}")

        changeColorWhenSelectPayment()

        // Start countdown timeout and voice
        timerTimeoutPayment.start()
        MainApplication.mAudioManager.soundPleaseTapCard()

        // Change Payment state
        AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
        AppContainer.CurrentTransaction.paymentModeType = PaymentModeType.KONBINI_WALLET

        // Locked cart
        AppContainer.CurrentTransaction.cartLocked()
    }

    private fun handleClickedCashPayment() {
        val validate = validateSelectPayment()
        if (!validate) return

        // Check click button many times
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }

        handleClickedSelectProductButton(true)

        lastTimeClicked = SystemClock.elapsedRealtime()
        LogUtils.logInfo("User clicked ${PaymentModeType.CASH.value}")

        changeColorWhenSelectPayment()

        // Start countdown timeout and voice
        MainApplication.mAudioManager.soundPleaseWaitCashierConfirm()

        // Change Payment state
        AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
        AppContainer.CurrentTransaction.paymentModeType = PaymentModeType.CASH

        displayMessage(getString(R.string.message_please_wait_cashier_confirm))

        // Locked cart
        AppContainer.CurrentTransaction.cartLocked()
    }

    private fun handleClickedCashPaymentWithTopUp() {
        // Validate
        if (binding.textInputEditTextTopUpCcw.text.toString().isEmpty() ||
            binding.textInputEditTextTopUpAmount.text.toString().isEmpty()
        ) {
            AlertDialogUtil.showWarning(
                getString(R.string.message_warning_ccw_id_and_amount_is_required),
                requireContext()
            )
            return
        }

        val currency = Currency.getInstance(Locale.getDefault())
        val symbol: String = currency.symbol

        AppContainer.CurrentTransaction.cardNFC = binding.textInputEditTextTopUpCcw.text.toString()
        AppContainer.CurrentTransaction.totalPrice =
            binding.textInputEditTextTopUpAmount.text.toString().replace(symbol, "").toFloat()
        handleClickedTopUpButton(isProcessingPayment = true)

        lastTimeClicked = SystemClock.elapsedRealtime()
        LogUtils.logInfo("User clicked ${PaymentModeType.CASH.value}")

        changeColorWhenSelectPayment()

        // Start countdown timeout and voice
        MainApplication.mAudioManager.soundPleaseWaitCashierConfirm()

        // Change Payment state
        AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
        AppContainer.CurrentTransaction.paymentModeType = PaymentModeType.CASH

        displayMessage(getString(R.string.message_please_wait_cashier_confirm))
    }

    private fun handleClickedDiscountButton() {
        lastTimeClicked = SystemClock.elapsedRealtime()
        LogUtils.logInfo("User clicked ${PaymentModeType.DISCOUNT.value}")

        timerTimeoutDiscount.cancel()
        timerTimeoutDiscount.start()

        MainApplication.mAudioManager.soundEnterDiscount()
        var message = getString(R.string.message_please_scan_barcode)
        if (AppSettings.Options.Discount.NFC) {
            message = getString(R.string.message_please_tap_membership_card)
        }
        pDialog = AlertDialogUtil.showProgress(
            message,
            requireContext()
        )

        LogUtils.logInfo("Message | $message")

        if (AppSettings.Options.Discount.Barcode) {
            pDialog.setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val pressedKey = event.unicodeChar.toChar()
                    barcode += pressedKey
                }

                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (barcode.length == 1) {
                        Timer().schedule(100) {
                            if (AppSettings.Options.AllowAdminCancelPayment && barcode.length == 1) {
                                Log.e("BARCODE_VALUE", barcode)
                                LogUtils.logInfo("User scan barcode | $barcode")
                                // Hide icon loading
                                hideDialogDiscount()
                                barcode = ""
                            }
                        }
                    }

                    when (event.keyCode) {
                        KeyEvent.KEYCODE_ENTER -> {
                            Log.e("BARCODE_VALUE", barcode)
                            LogUtils.logInfo("User scan barcode | $barcode")

                            barcode = barcode.split("\n")[0]
                            AppContainer.CurrentTransaction.ccwId1 = barcode
                            // Hide icon loading
                            hideDialogDiscount()
                            AppContainer.CurrentTransaction.ccwId1 = barcode
                            listenerDiscount()
                            barcode = ""
                        }
                    }
                }
                false
            })
        }

        // TODO: Start TrungPQ add to test
        // AppContainer.CurrentTransaction.ccwId1 = "8d2ed739"
        // listenerDiscount()
    }

    private fun handleClickedSelectProductButton(isHide: Boolean = false) {
        if (AppContainer.CurrentTransaction.paymentState != PaymentState.Init &&
            AppContainer.CurrentTransaction.paymentState != PaymentState.Preparing
        ) return
        var layoutWeightPayment = 4f
        if (!binding.layoutCategoryProduct.isVisible && !isHide) {
            layoutWeightPayment = 8f
        } else {
            AppContainer.CurrentTransaction.selectedCategory = 0
            initRecyclerViewCategories()
            initRecyclerViewProducts()
        }

        // Set Layout Weight
        binding.selectPayment.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
            layoutWeightPayment
        )

        if (isHide) {
            binding.layoutCategoryProduct.isVisible = false
            binding.layoutCountAndTotal.isVisible = true
            binding.layoutMessageTitle.isVisible = true
        } else {
            // Set show/hide
            binding.layoutCategoryProduct.isVisible = !binding.layoutCategoryProduct.isVisible
            binding.layoutCountAndTotal.isVisible = !binding.layoutCountAndTotal.isVisible
            binding.layoutMessageTitle.isVisible = !binding.layoutMessageTitle.isVisible
        }
    }

    /**
     * Goto login
     *
     */
    private fun gotoLogin() {
        view?.post {
            findNavController().navigate(
                R.id.action_magicPlateFragment_to_loginFragment
            )
        }
    }

    /**
     * Cancel payment
     *
     */
    private fun cancelPayment(isTimeout: Boolean = false) {
        // Hide icon loading
        hideDialogDiscount()

        // Handle Hardware
        if (AppContainer.CurrentTransaction.paymentModeType == PaymentModeType.MASTER_CARD
            || AppContainer.CurrentTransaction.paymentModeType == PaymentModeType.EZ_LINK
            || AppContainer.CurrentTransaction.paymentModeType == PaymentModeType.PAY_NOW
        ) {
            IM30Interface.instance.cancel()
        }

        // Reset temporary info
        AppContainer.CurrentTransaction.resetTemporaryInfo()
        AppContainer.CurrentTransaction.paymentState = PaymentState.Init

        // Refresh cart
        refreshCart()

        // Show blink for cancel payment
        setBlink(AlarmType.CANCEL)

        // Reset countdown timeout payment
        timerTimeoutPayment.cancel()
        timeout = AppSettings.Options.Payment.Timeout

        // Show message
        if (isTimeout) {
            LogUtils.logInfo(getString(R.string.message_payment_timeout))
            MainApplication.mAudioManager.soundPaymentTimeout()
            displayMessage(getString(R.string.message_payment_timeout))
        } else {
            LogUtils.logInfo(getString(R.string.message_payment_cancelled))
            MainApplication.mAudioManager.soundPaymentCancelled()
            //MainApplication.mAudioManager.soundPaymentCancelled()
            displayMessage(getString(R.string.message_payment_cancelled))
        }
        val message = getString(R.string.message_put_plate_on_the_tray)
        resetMessage(message, 0)
    }

    // region ================Handle UI================
    /**
     * Show hide loading
     *
     * @param show
     */
    private fun showHideLoading(show: Boolean) {
        LogUtils.logInfo("Show hide loading | $show")
        if (show) {
            binding.loadingPanel.visibility = View.VISIBLE
            binding.contentPanel.visibility = View.GONE
        } else {
            binding.loadingPanel.visibility = View.GONE
            binding.contentPanel.visibility = View.VISIBLE
        }
    }

    /**
     * Display current TimeBlock
     */
    private fun displayTimeBlock() {
        val currentTimeBock = AppContainer.GlobalVariable.currentTimeBock
        var textValue = "N/A: --/-- \n ${MainApplication.currentVersion}"

        if (currentTimeBock != null) {
            textValue = "${currentTimeBock.timeBlockTitle}: " +
                    "${convertStringToShortTime(currentTimeBock.fromHour)} - " +
                    convertStringToShortTime(currentTimeBock.toHour) +
                    "\n ${MainApplication.currentVersion}"
        }
        binding.rfidCurrentTimeBlock.text = textValue
    }

    /**
     * Display Total Price
     */
    private fun displayTotal() {
        val totalPrice = AppContainer.CurrentTransaction.totalPrice
        binding.rfidTotalCount.text = formatCurrency(totalPrice)
    }

    /**
     * Display payment mode
     *
     */
    private fun displayPaymentMode() {
        if (AppSettings.Options.NotAllowWalletNonRfid && AppContainer.CurrentTransaction.cart.find { cartEntity ->
                cartEntity.strEPC.isEmpty()
            } != null)
            initRecyclerViewPayments(checkNonRFID = true)
    }

    /**
     * Display Count Items
     */
    private fun displayCountItem() {
        val countItems = AppContainer.CurrentTransaction.countItems
        if (countItems < 1) {
            // Hide icon loading
            hideDialogDiscount()
        }
        binding.rfidItemCount.text = countItems.toString()
    }

    /**
     * Display message
     *
     * @param message
     */
    private fun displayMessage(message: String, isLoading: Boolean = false) {
        binding.rfidMessageTitle.text = message
        when (message) {
            getString(R.string.message_cash_approved_payment_admin) -> {
                binding.rfidCancelPayment.visibility = View.GONE
                binding.spinKitMessage.visibility = View.VISIBLE
            }
            else -> {
                if (AppContainer.CurrentTransaction.paymentState == PaymentState.ReadyToPay
                    && AppSettings.Options.ShowCancelPaymentButton
                ) {
                    binding.rfidCancelPayment.visibility = View.VISIBLE
                } else {
                    binding.rfidCancelPayment.visibility = View.GONE
                }
                if (isLoading) {
                    binding.spinKitMessage.visibility = View.VISIBLE
                } else {
                    binding.spinKitMessage.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Reset message
     *
     * @param message
     * @param voice
     */
    private fun resetMessage(message: String, voice: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            displayMessage(message)
            when (voice) {
                R.raw.please_tap_card_again -> {
                    MainApplication.mAudioManager.soundPleaseTapCardAgain()
                }
            }
        }
    }

    /**
     * Display cart
     *
     */
    private fun displayCart() {
        val cart = AppContainer.CurrentTransaction.cart
        cart.sortBy { tagEntity -> tagEntity.strEPC }
        cartAdapter.setItems(ArrayList(cart))
        //LogUtils.logInfo("Display Cart: ${gson.toJson(cart)}")
    }

    /**
     * Refresh cart
     *
     */
    private fun refreshCart() {
        displayCart()
        displayCountItem()
        displayTotal()
        displayPaymentMode()
    }

    /**
     * Change color
     *
     * @param alarmType
     */
    private fun setBlink(alarmType: AlarmType) {
        when (alarmType) {
            AlarmType.SUCCESS -> {
                binding.rfidProducts.blink(Color.GREEN, 3, 500L)
                binding.layoutMessageTitle.blink(Color.GREEN, 3, 500L)
                //binding.rfidMessageTitle.blink(Color.GREEN, 3, 500L)
                binding.rfidItemCount.blink(Color.GREEN, 3, 500L)
                binding.rfidTotalCount.blink(Color.GREEN, 3, 500L)
                binding.selectPayment.blink(Color.GREEN, 3, 500L)
            }
            AlarmType.WARNING -> {
                binding.rfidProducts.blink(Color.YELLOW, 3, 500L)
                binding.layoutMessageTitle.blink(Color.YELLOW, 3, 500L)
                //binding.rfidMessageTitle.blink(Color.YELLOW, 3, 500L)
                binding.rfidItemCount.blink(Color.YELLOW, 3, 500L)
                binding.rfidTotalCount.blink(Color.YELLOW, 3, 500L)
                binding.selectPayment.blink(Color.YELLOW, 3, 500L)
            }
            AlarmType.ERROR -> {
                binding.rfidProducts.blink(Color.RED, 3, 500L)
                binding.layoutMessageTitle.blink(Color.RED, 3, 500L)
                //binding.rfidMessageTitle.blink(Color.RED, 3, 500L)
                binding.rfidItemCount.blink(Color.RED, 3, 500L)
                binding.rfidTotalCount.blink(Color.RED, 3, 500L)
                binding.selectPayment.blink(Color.RED, 3, 500L)
            }
            else -> {
                binding.rfidProducts.blink(Color.GRAY, 3, 500L)
                binding.layoutMessageTitle.blink(Color.GRAY, 3, 500L)
                //binding.rfidMessageTitle.blink(Color.GRAY, 3, 500L)
                binding.rfidItemCount.blink(Color.GRAY, 3, 500L)
                binding.rfidTotalCount.blink(Color.GRAY, 3, 500L)
                binding.selectPayment.blink(Color.GRAY, 3, 500L)
            }
        }
    }

    /**
     * Change color when select payment
     *
     */
    private fun changeColorWhenSelectPayment() {
        binding.rfidProducts.setBackgroundResource(R.color.yellow)
    }

    private fun hideDialogDiscount() {
        // Hide icon loading
        if (this::pDialog.isInitialized) {
            if (pDialog.isShowing) {
                pDialog.dismiss()
            }
        }
    }
    // endregion

    // region ================Listener Functions================
    private fun handleTopUpSuccess(message: String) {
        LogUtils.logInfo("Top Up Success")
        setBlink(AlarmType.SUCCESS)
        displayMessage(message)
        MainApplication.mAudioManager.soundPaymentSuccess()
        AppContainer.CurrentTransaction.paymentState = PaymentState.Success

        if (AppSettings.Options.Printer.Bluetooth || AppSettings.Options.Printer.USB) {
            // Print Receipt
            LogUtils.logInfo("Start Print top up receipt")
            printTopUpReceipt()
        }

        AppContainer.CurrentTransaction.resetTemporaryInfo()

        val message = getString(R.string.message_put_plate_on_the_tray)
        resetMessage(message, 0)
    }

    private fun handleTopUpError(message: String) {
        setBlink(AlarmType.ERROR)
        displayMessage(message)
        MainApplication.mAudioManager.soundBuzzer()
        AppContainer.CurrentTransaction.paymentState = PaymentState.Error

        val message = getString(R.string.message_put_plate_on_the_tray)
        resetMessage(message, 0)
        AppContainer.CurrentTransaction.paymentState = PaymentState.Preparing
    }

    /**
     * Handle payment success
     *
     */
    private fun handlePaymentSuccess(cardType: String) {
        LogUtils.logInfo("Payment Success")
        if (AppSettings.Options.Payment.DeviceType == PaymentDeviceType.IM30.value) {
            // Reset countdown timeout payment
            timerTimeoutPayment.cancel()
            timeout = AppSettings.Options.Payment.Timeout

            // MainApplication.mAudioManager.soundProcessingPayment()
            AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
        }
        setBlink(AlarmType.SUCCESS)
        displayMessage(
            String.format(
                resources.getString(R.string.message_success_payment),
                formatCurrency(AppContainer.CurrentTransaction.totalPrice)
            )
        )
        MainApplication.mAudioManager.soundPaymentSuccess()
        AppContainer.CurrentTransaction.paymentState = PaymentState.Success
        //Log.e("EKRON", "PaymentState.Success")
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        // Save transaction
        val transaction = TransactionEntity(
            0,
            uuid = UUID.randomUUID().toString(),
            amount = AppContainer.CurrentTransaction.totalPrice.toString(),
            discountPercent = "0.0",
            taxPercent = "0.0",
            buyer = "n/a",
            beginImage = "n/a",
            endImage = "n/a",
            details = gson.toJson(AppContainer.CurrentTransaction.cartLocked),
            paymentDetail = AppContainer.CurrentTransaction.paymentModeType!!.value,
            paymentTime = currentTime.toString(),
            paymentState = PaymentState.Success.name,
            paymentType = AppContainer.CurrentTransaction.paymentModeType!!.value,
            cardType = cardType,
            cardNumber = AppContainer.CurrentTransaction.cardNFC,
            approveCode = "n/a",
            note = "n/a"
        )
        transaction.dateCreated = currentTime.toString()
        viewModel.insert(transaction)

        if (AppSettings.Options.Printer.Bluetooth || AppSettings.Options.Printer.USB) {
            // Print Receipt
            LogUtils.logInfo("Start Print receipt")
            val cartLocked: MutableList<CartEntity> =
                ArrayList(AppContainer.CurrentTransaction.cartLocked)
            printReceipt(cartLocked, AppContainer.CurrentTransaction.currentDiscount)
        }

        val message = getString(R.string.message_put_plate_on_the_tray)
        resetMessage(message, 0)
        AppContainer.CurrentTransaction.resetTemporaryInfo()
        // Refresh cart
        refreshCart()
    }

    /**
     * Handle payment error
     *
     * @param _message
     */
    private fun handlePaymentError(_message: String) {
        LogUtils.logInfo("Payment Error | $_message")
        // Reset countdown timeout payment
        timerTimeoutPayment.cancel()
        timeout = AppSettings.Options.Payment.Timeout

        setBlink(AlarmType.ERROR)
        displayMessage(ErrorCodeIM30.handleMessageIuc(_message, requireContext()))
        MainApplication.mAudioManager.soundBuzzer()
        AppContainer.CurrentTransaction.paymentState = PaymentState.Init

        val message = getString(R.string.message_put_plate_on_the_tray)
        resetMessage(message, 0)
        AppContainer.CurrentTransaction.paymentState = PaymentState.Preparing
    }

    /**
     * Listener master card
     *
     */
    private fun listenerMasterCard() {
        val amount = (AppContainer.CurrentTransaction.totalPrice.toDouble() * 100).roundToInt()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                IM30Interface.instance.sale(amount, { saleResponse ->
                    // Success
                    activity?.runOnUiThread {
                        handlePaymentSuccess(saleResponse.cardType)
                    }
                }, { _message ->
                    // Error
                    activity?.runOnUiThread {
                        handlePaymentError(_message)
                    }
                }, { _message ->
                    // Callback
                    activity?.runOnUiThread {
                        // Reset countdown timeout payment
                        timerTimeoutPayment.cancel()
                        timeout = AppSettings.Options.Payment.Timeout

                        // MainApplication.mAudioManager.soundProcessingPayment()
                        AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress

//                        when (AppSettings.Options.Payment.DeviceType) {
//                            PaymentDeviceType.IUC.value -> { // IUC
//                                if (_message.lowercase().contains("PLEASE WAIT".lowercase())) {
//                                    // Reset countdown timeout payment
//                                    timerTimeoutPayment.cancel()
//                                    timeout = AppSettings.Options.Payment.Timeout
//
//                                    // MainApplication.mAudioManager.soundProcessingPayment()
//                                    AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
//                                }
//                            }
//                            else -> { // IM30
//                                // Reset countdown timeout payment
//                                timerTimeoutPayment.cancel()
//                                timeout = AppSettings.Options.Payment.Timeout
//
//                                // MainApplication.mAudioManager.soundProcessingPayment()
//                                AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
//                            }
//                        }

                        Log.e(TAG, _message)
                        LogUtils.logInfo(_message)
                        displayMessage(_message)
                    }
                }, {
                    // Cancel
                    activity?.runOnUiThread {
                        LogUtils.logInfo("MasterCard | Cancel payment")
                        cancelPayment()
                    }
                })
            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }

    /**
     * Listener ez link
     *
     */
    private fun listenerEzLink() {
        val amount = (AppContainer.CurrentTransaction.totalPrice.toDouble() * 100).roundToInt()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                IM30Interface.instance.cpas(amount, { saleResponse ->
                    // Success
                    activity?.runOnUiThread {
                        handlePaymentSuccess(saleResponse.cardType)
                    }
                }, { _message ->
                    // Error
                    activity?.runOnUiThread {
                        handlePaymentError(_message)
                    }
                }, { _message ->
                    // Callback
                    activity?.runOnUiThread {
                        // Reset countdown timeout payment
                        timerTimeoutPayment.cancel()
                        timeout = AppSettings.Options.Payment.Timeout

                        // MainApplication.mAudioManager.soundProcessingPayment()
                        AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress

//                        when (AppSettings.Options.Payment.DeviceType) {
//                            PaymentDeviceType.IUC.value -> { // IUC
//                                if (_message.lowercase().contains("REMOVE CARD".lowercase())) {
//                                    // Reset countdown timeout payment
//                                    timerTimeoutPayment.cancel()
//                                    timeout = AppSettings.Options.Payment.Timeout
//
//                                    // MainApplication.mAudioManager.soundProcessingPayment()
//                                    AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
//                                }
//                            }
//                            else -> { // IM30
//                                // Reset countdown timeout payment
//                                timerTimeoutPayment.cancel()
//                                timeout = AppSettings.Options.Payment.Timeout
//
//                                // MainApplication.mAudioManager.soundProcessingPayment()
//                                AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
//                            }
//                        }

                        Log.e(TAG, _message)
                        LogUtils.logInfo(_message)
                        displayMessage(_message)
                    }
                }, {
                    // Cancel
                    activity?.runOnUiThread {
                        LogUtils.logInfo("EzLink | Cancel payment")
                        cancelPayment()
                    }
                })
            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }

    /**
     * Listener pay now
     *
     */
    private fun listenerPayNow() {
        val amount = (AppContainer.CurrentTransaction.totalPrice.toDouble() * 100).roundToInt()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                IM30Interface.instance.mpqr(
                    amount,
                    IM30Interface.MpqrWalletLabel.DBSMAX_PAYNOW,
                    { saleResponse ->
                        // Success
                        activity?.runOnUiThread {
                            handlePaymentSuccess(saleResponse.cardType)
                        }
                    },
                    { _message ->
                        // Error
                        activity?.runOnUiThread {
                            handlePaymentError(_message)
                        }
                    },
                    { _message ->
                        // Callback
                        activity?.runOnUiThread {
                            // Reset countdown timeout payment
                            timerTimeoutPayment.cancel()
                            timeout = AppSettings.Options.Payment.Timeout

                            // MainApplication.mAudioManager.soundProcessingPayment()
                            AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress

//                        when (AppSettings.Options.Payment.DeviceType) {
//                            PaymentDeviceType.IUC.value -> { // IUC
//                                if (_message.lowercase().contains("PLEASE WAIT".lowercase())) {
//                                    // Reset countdown timeout payment
//                                    timerTimeoutPayment.cancel()
//                                    timeout = AppSettings.Options.Payment.Timeout
//
//                                    // MainApplication.mAudioManager.soundProcessingPayment()
//                                    AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
//                                }
//                            }
//                            else -> { // IM30
//                                // Reset countdown timeout payment
//                                timerTimeoutPayment.cancel()
//                                timeout = AppSettings.Options.Payment.Timeout
//
//                                // MainApplication.mAudioManager.soundProcessingPayment()
//                                AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
//                            }
//                        }

                            Log.e(TAG, _message)
                            LogUtils.logInfo(_message)
                            displayMessage(_message)
                        }
                    },
                    {
                        // Cancel
                        activity?.runOnUiThread {
                            LogUtils.logInfo("PayNow | Cancel payment")
                            cancelPayment()
                        }
                    })
            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }

    /**
     * Listener acs reader
     *
     */
    private fun listenerAcsReader() {
        try {
            MainApplication.mReaderASC.setOnStateChangeListener { slotNum, prevState, currState ->
                var prevState = prevState
                var currState = currState
                if (prevState < Reader.CARD_UNKNOWN
                    || prevState > Reader.CARD_SPECIFIC
                ) {
                    prevState = Reader.CARD_UNKNOWN
                }
                if (currState < Reader.CARD_UNKNOWN
                    || currState > Reader.CARD_SPECIFIC
                ) {
                    currState = Reader.CARD_UNKNOWN
                }

                if (prevState == Reader.CARD_ABSENT && currState == Reader.CARD_PRESENT) {
                    if (slotNum == 0) {
                        // The APDU (smart card application protocol data unit) byte array to get the UID from the card
                        // Command  | Class | INS | P1 | P2 | Le
                        // Get Data |   FF  | CA  | 00 | 00 | 00
                        val command = byteArrayOf(
                            0xFF.toByte(),
                            0xCA.toByte(),
                            0x00.toByte(),
                            0x00.toByte(),
                            0x00.toByte()
                        )

                        // The byte array to contain the response
                        val response = ByteArray(1024)

                        // In order to set the Get Data command to the card, we need to send a warm reset, followed by setting
                        // the communications protocol.
                        try {
                            MainApplication.mReaderASC.power(slotNum, Reader.CARD_WARM_RESET)
                            MainApplication.mReaderASC.setProtocol(
                                slotNum,
                                Reader.PROTOCOL_T0 or Reader.PROTOCOL_T1
                            )

                            // Send the command to the reader
                            var responseLength = MainApplication.mReaderASC.transmit(
                                slotNum,
                                command,
                                command.size,
                                response,
                                response.size
                            )

                            // We appear to be getting all 9 bytes of a 7 byte identifier.  Since 9 would be considered a too
                            // large value of 7, we drop the last 2 bytes
                            if (responseLength > 6) {
                                responseLength = 7;
                            }

                            // If we got a response, process it
                            if (responseLength > 0) {
                                val data = response.take(responseLength).toByteArray()
                                // Convert the byte array to a hex string
                                val uid = data.toHexString()
                                if (AppSettings.Options.AcsReader == AcsReaderType.WHITE.value) {
                                    AppContainer.CurrentTransaction.cardNFC =
                                        uid.substring(0, uid.length - 4)
                                } else {
                                    AppContainer.CurrentTransaction.cardNFC = uid
                                }

                                LogUtils.logInfo("User tap card: ${AppContainer.CurrentTransaction.cardNFC}")

                                val paymentState = AppContainer.CurrentTransaction.paymentState
                                if (paymentState == PaymentState.ReadyToPay
                                    && AppContainer.CurrentTransaction.paymentModeType == PaymentModeType.KONBINI_WALLET
                                ) {
                                    AppContainer.CurrentTransaction.paymentState =
                                        PaymentState.InProgress
                                    viewModel.debit()
                                }

                                // Discount
                                if ((paymentState == PaymentState.Init
                                            || paymentState == PaymentState.Preparing)
                                    && AppSettings.Options.Discount.NFC
                                ) {
                                    // Hide icon loading
                                    hideDialogDiscount()
                                    AppContainer.CurrentTransaction.ccwId1 =
                                        AppContainer.CurrentTransaction.cardNFC
                                    listenerDiscount()
                                }
                            }
                        } catch (readerException: ReaderException) {
                            listenerAcsReader()
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            LogUtils.logInfo("Exception: ${ex.toString()}")
        }
    }

    @JvmName("toHexString1")
    fun ByteArray.toHexString(): String {
        return this.joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }

    private fun listenerDiscount() {
        lifecycleScope.launch {
            if (AppSettings.Options.Discount.DiscountByFormat && AppSettings.Options.MachineTypeActivated == MachineType.DISCOUNT_MODE.value) {
                val validate = validateFormatOfDiscount()
                if (validate) {
                    AppContainer.CurrentTransaction.currentDiscount = 1F
                    AppContainer.CurrentTransaction.refreshCart()
                    refreshCart()
                } else {
                    AlertDialogUtil.showWarning(
                        String.format(
                            getString(R.string.message_error_wrong_format),
                            AppContainer.CurrentTransaction.ccwId1
                        ),
                        requireContext()
                    )
                }
            } else {
                var discounts: MutableList<DiscountEntity> = mutableListOf()
                if (AppSettings.Options.DiscountList.isNotEmpty()) {
                    discounts = gson.fromJson(
                        AppSettings.Options.DiscountList,
                        Array<DiscountEntity>::class.java
                    ).toMutableList()
                    discounts.sortByDescending { discountEntity -> discountEntity.discountValue }
                }

                val userEntity = findUserByCcwId1()
                if (userEntity == null) {
                    AlertDialogUtil.showError(
                        getString(R.string.message_error_user_not_found),
                        requireContext()
                    )
                } else {
                    var roles: MutableList<String> = mutableListOf()
                    if (userEntity.roles.isNotEmpty()) {
                        roles = userEntity.roles.split(",").map { it -> it.trim() }.toMutableList()
                    }
                    if (roles.isNotEmpty()) {
                        run outForeach@{
                            discounts.forEach { discountEntity ->
                                if (roles.contains(discountEntity.roleName)) {
                                    Log.e(TAG, discountEntity.discountValue)
                                    AppContainer.CurrentTransaction.currentDiscount =
                                        discountEntity.discountValue.toFloat()
                                    AppContainer.CurrentTransaction.refreshCart()
                                    refreshCart()
                                    return@outForeach
                                }
                            }
                        }
                    } else {
                        LogUtils.logInfo("Roles is empty")
                    }
                }
            }
        }
    }

    private fun findUserByCcwId1(): UserEntity? {
        return AppContainer.GlobalVariable.listUsers.find { userEntity ->
            AppContainer.CurrentTransaction.ccwId1 == userEntity.ccwId1
        }
    }
    // endregion

    // region ================Validation Functions================
    /**
     * Validate cart is empty
     *
     * @return
     */
    private fun validateCartIsEmpty(): Boolean {
        return AppContainer.CurrentTransaction.cart.isEmpty()
    }

    /**
     * Validate select payment
     *
     * @return
     */
    private fun validateSelectPayment(): Boolean {
        val validate = validateCartIsEmpty()
        if (validate) {
            LogUtils.logInfo(getString(R.string.message_warning_cart_is_empty))
            // Show message warning cart is empty
            displayMessage(getString(R.string.message_warning_cart_is_empty))
            setBlink(AlarmType.ERROR)
            MainApplication.mAudioManager.soundCartIsEmpty()

            val message = getString(R.string.message_put_plate_on_the_tray)
            resetMessage(message, 0)
        }
        return !validate
    }

    private fun validateFormatOfDiscount(): Boolean {
        val length = AppContainer.CurrentTransaction.ccwId1.length
        if (AppSettings.Options.Discount.LengthFormat > 0) {
            if (length != AppSettings.Options.Discount.LengthFormat) {
                LogUtils.logInfo("Discount | ${AppContainer.CurrentTransaction.ccwId1} wrong format!!!")
                return false
            }
        }

        if (AppSettings.Options.Discount.PrefixFormat.isNotEmpty()) {
            val pattern = Regex("^${AppSettings.Options.Discount.PrefixFormat}")
            if (!pattern.containsMatchIn(AppContainer.CurrentTransaction.ccwId1)) {
                LogUtils.logInfo("Discount | ${AppContainer.CurrentTransaction.ccwId1} wrong format!!!")
                return false
            }
        }

        if (AppSettings.Options.Discount.SuffixesFormat.isNotEmpty()) {
            val pattern = Regex("${AppSettings.Options.Discount.SuffixesFormat}$")
            if (!pattern.containsMatchIn(AppContainer.CurrentTransaction.ccwId1)) {
                LogUtils.logInfo("Discount | ${AppContainer.CurrentTransaction.ccwId1} wrong format!!!")
                return false
            }
        }
        return true
    }
    // endregion

    // region ================Print Receipt================
    private fun printTopUpReceipt() {

    }

    private fun printReceipt(cartLocked: MutableList<CartEntity>, currentDiscount: Float) {
        viewLifecycleOwner.lifecycleScope.launch {
            contentReceipt = ""
            val lastNumber = viewModel.getLastTransactionId()
            if (lastNumber != null) {
                orderNumber = lastNumber + 1
            }

            if (AppSettings.Options.Printer.Bluetooth) {
                // Bluetooth
                printReceiptBluetooth(cartLocked, currentDiscount)
            } else {
                if (AppSettings.Options.Printer.TCP) {
                    // TCP
                    printReceiptTCP(cartLocked, currentDiscount)
                } else {
                    contentReceipt = formatReceipt(cartLocked, currentDiscount)
                    Log.e("PRINTER", contentReceipt)
                    // USB
                    printReceiptUSB()
                }
            }
        }
    }

    private fun printReceiptBluetooth(cartLocked: MutableList<CartEntity>, currentDiscount: Float) {

        val device = BluetoothPrintersConnections.selectFirstPaired()
        val printer =
            EscPosPrinter(device, 203, /*AppSettings.ReceiptPrinter.WidthPaper.toFloat()*/48f, 32)
        val content = formatReceipt(cartLocked, currentDiscount)
        printer.printFormattedTextAndCut(content)
    }

    private fun printReceiptTCP(cartLocked: MutableList<CartEntity>, currentDiscount: Float) {
        val ip = AppSettings.ReceiptPrinter.TCP
        if (ip.isNotEmpty()) {
            val printer = EscPosPrinter(
                TcpConnection(ip, 9300, 15),
                203,
                AppSettings.ReceiptPrinter.WidthPaper.toFloat(),
                32
            )
            val content = formatReceipt(cartLocked, currentDiscount)
            printer.printFormattedTextAndCut(content)
        } else {
            AlertDialogUtil.showError(
                getString(R.string.message_error_TCP_is_empty),
                requireContext()
            )
        }
    }

    private fun printReceiptUSB() {
        val usbConnections = UsbPrintersConnections(requireContext()).list
        usbConnections?.forEach usbConnections@{ usbConnection ->
            if (usbConnection.device.manufacturerName?.contains("printer") == true) {
                val usbManager =
                    requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager?
                if (usbConnection != null && usbManager != null) {
                    val permissionIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        0,
                        Intent(ACTION_USB_PERMISSION),
                        0
                    )
                    val filter = IntentFilter(ACTION_USB_PERMISSION)
                    requireActivity().registerReceiver(usbReceiver, filter)
                    usbManager.requestPermission(usbConnection.device, permissionIntent)
                } else {
                    AlertDialogUtil.showError(
                        getString(R.string.message_error_printer_not_found),
                        requireContext()
                    )
                }
                return@usbConnections
            }
        }
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action = intent.action
                if (ACTION_USB_PERMISSION == action) {
                    synchronized(this) {
                        val usbManager =
                            requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager?
                        val usbDevice =
                            intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (usbManager != null && usbDevice != null) {
                                val printer =
                                    EscPosPrinter(
                                        UsbConnection(usbManager, usbDevice),
                                        203,
                                        AppSettings.ReceiptPrinter.WidthPaper.toFloat(),
                                        32
                                    )
                                printer.printFormattedText(contentReceipt)
                                printer.printFormattedTextAndCut("")
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                LogUtils.logError(ex)
            }
        }
    }

    private fun formatReceipt(cartLocked: MutableList<CartEntity>, currentDiscount: Float): String {
        return "[C]<font size='tall'>Store: ${AppSettings.Machine.Store}</font>\n" +
                "[C]<font size='tall'>Terminal: ${AppSettings.Machine.Terminal}</font>\n" +
                "[C]<font size='tall'>Date: ${Date()}</font>\n" +
                "[C]<font size='big'>RECEIPT #${"%06d".format(orderNumber)}</font>\n" +
                formatContent(cartLocked, currentDiscount) +
                "[L]<font size='normal'>Tel: ${AppSettings.Company.Tel}</font>\n" +
                "[L]<font size='normal'>Email: ${AppSettings.Company.Email}</font>\n" +
                "[L]<font size='normal'>Address: ${AppSettings.Company.Address}</font>\n" +
                "[C]<font size='tall'><b>Thank you!!!</b></font>\n" +
                "[L]<font size='tall'>Membership :</font>\n" +
                "[L]Display Name: ${if (displayName.isEmpty()) "N/A" else displayName}\n" +
                "[L]Balance: ${if (balance == 0F) "N/A" else formatCurrency(balance)}\n" +
                "[L]\n" +
                "[C]<barcode type='ean13' height='10'>${"%012d".format(orderNumber)}</barcode>\n" +
                //"[C]<qrcode size='20'>831254784551</qrcode>" +
                "[L]\n" +
                "[L]\n" +
                "[L]\n"
    }

    private fun formatContent(cartLocked: MutableList<CartEntity>, currentDiscount: Float): String {
        var total = 0F
        var items = ""
        cartLocked.forEach { cartEntity ->
            var price = formatCurrency(cartEntity.price.toFloat() * cartEntity.quantity)
            if (currentDiscount > 0) {
                price = formatCurrency(cartEntity.salePrice.toFloat() * cartEntity.quantity)
            }
            val strItem = "[L]<b>${cartEntity.productName}</b>[C]${cartEntity.quantity}[R]$price\n"
            items += strItem
            total += (cartEntity.price.toFloat() * cartEntity.quantity)
        }
        return "[C]================================\n" +
                "[L]\n" +
                items +
                "[L]\n" +
                "[C]--------------------------------\n" +
                "[R]TOTAL PRICE :[R]${formatCurrency(total)}\n" +
                "[L]\n" +
                "[C]================================\n"
    }
    // endregion

    // region ================Admin control by keyboard================
    private fun adminCancelPayment(pressedKey: String) {
        if (AppSettings.Options.AllowAdminCancelPayment) {
            val currentKeyCode = AppSettings.Options.KeyCodeCancelPayment
            val isCorrect = CommonUtil.checkKeyCodeExists(pressedKey, currentKeyCode)

            if (isCorrect) {
                val state = AppContainer.CurrentTransaction.paymentState
                if (state == PaymentState.ReadyToPay) {
                    displayMessage(getString(R.string.message_cancel_payment_admin))
                    LogUtils.logInfo(getString(R.string.message_cancel_payment_admin))
                    cancelPayment()
                }
            }
        }
    }

    private fun adminCashPaymentApproval(pressedKey: String) {
        if (AppSettings.Options.AllowAdminCashPaymentApproval) {
            val currentKeyCode = AppSettings.Options.KeyCodeCashPaymentApproval
            val isCorrect = CommonUtil.checkKeyCodeExists(pressedKey, currentKeyCode)

            if (isCorrect) {
                if (AppContainer.CurrentTransaction.isTopUp) {
                    AppContainer.CurrentTransaction.paymentModeType = PaymentModeType.TOP_UP
                    viewModel.credit("")
                } else {
                    val state = AppContainer.CurrentTransaction.paymentState
                    if (state == PaymentState.ReadyToPay) {
                        displayMessage(getString(R.string.message_cash_approved_payment_admin))
                        LogUtils.logInfo(getString(R.string.message_cash_approved_payment_admin))

                        handlePaymentSuccess(PaymentModeType.CASH.value)
                    }
                }
            }
        }
    }

    private fun adminDiscountApproval(pressedKey: String) {
        if (AppSettings.Options.AllowAdminDiscountApproval) {
            val currentKeyCode = AppSettings.Options.KeyCodeDiscountApproval
            val isCorrect = CommonUtil.checkKeyCodeExists(pressedKey, currentKeyCode)

            if (isCorrect) {
                val state = AppContainer.CurrentTransaction.paymentState
                if (state == PaymentState.Init || state == PaymentState.Preparing) {
                    displayMessage(getString(R.string.message_discount_approved_payment_admin))
                    LogUtils.logInfo(getString(R.string.message_discount_approved_payment_admin))

                    AppContainer.CurrentTransaction.currentDiscount = 1f
                    AppContainer.CurrentTransaction.refreshCart()
                    refreshCart()
                }
            }
        }
    }
    // endregion
}