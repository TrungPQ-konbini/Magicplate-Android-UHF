package com.konbini.magicplateuhf.ui.sales.magicPlate

import android.app.PendingIntent
import android.content.*
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Parcelable
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.developer.kalert.KAlertDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.*
import com.konbini.magicplateuhf.data.entities.*
import com.konbini.magicplateuhf.data.enum.*
import com.konbini.magicplateuhf.data.remote.product.response.Option
import com.konbini.magicplateuhf.databinding.FragmentMagicPlateBinding
import com.konbini.magicplateuhf.hardware.IM30Interface
import com.konbini.magicplateuhf.ui.settings.SettingsViewModel
import com.konbini.magicplateuhf.utils.*
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.blink
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.convertStringToShortTime
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.formatCurrency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

@AndroidEntryPoint
class MagicPlateFragment : Fragment(), PaymentAdapter.ItemListener, CartAdapter.ItemListener {

    companion object {
        const val TAG = "MagicPlateFragment"
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

        var displayName = ""
        var balance = 0F
    }

    private var orderNumber = 0
    private val gson = Gson()

    // Variable for Discount
    private var barcode: String = ""
    private lateinit var pDialog: KAlertDialog

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "REFRESH_TAGS" -> {
                    when (AppContainer.CurrentTransaction.paymentState) {
                        /**
                         * TODO: The reason we had ALARM last time was because people press Payment then walk away.
                         *       Can we have the Alarm, but make it ring only if we detect all tags/plates removed while we are still in PAYMENT mode.
                         */
                        PaymentState.ReadyToPay,
                        PaymentState.InProgress -> {
                            // Check Cart Locked Change
                            if (AppContainer.CurrentTransaction.listEPC.isEmpty()) {
                                AudioManager.instance.soundBuzzer()
                                setBlink(AlarmType.ERROR)
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
                    // Add to Mask real-time reading tags
                    var content = String.format(
                        getString(R.string.title_count_n_tags),
                        AppContainer.CurrentTransaction.listTagEntity.size
                    )
                    content += "\n\n"
                    AppContainer.CurrentTransaction.listTagEntity.forEach { tagEntity ->
                        content += "${tagEntity.strEPC} | ${tagEntity.plateModelTitle} \n"
                    }
                    binding.rfidRealTimeTags.text = content
                }
                "NEW_BARCODE" -> {
                    val paymentState = AppContainer.CurrentTransaction.paymentState
                    if (paymentState == PaymentState.InProgress || paymentState == PaymentState.ReadyToPay) {
                        AudioManager.instance.soundDoNotChangeItem()
                        return
                    }
                    val barcode = AppContainer.CurrentTransaction.barcode.split("\n")[0]
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
                            price = product.price,
                            productName = product.name,
                            plateModelName = "",
                            plateModelCode = "",
                            timeBlockTitle = "",
                            quantity = 1,
                            options = product.options
                        )
                    }
                }
                "ACCEPT_OPTIONS" -> {
                    // Refresh cart
                    refreshCart()
                }
                "MQTT_SYNC_DATA" -> {
                    viewModelSettings.syncAll()
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

    private var clickedTitleHeaderIndex = 0
    private var clickedTitleModel = 0
    private var clickedTitleTotal = 0

    private lateinit var cartAdapter: CartAdapter
    private lateinit var paymentAdapter: PaymentAdapter
    private var listPaymentType: MutableList<String> = mutableListOf()

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
        setupRecyclerView()
        setupObservers()
        setupActions()
        initData()
        listenerAcsReader()
    }

    override fun onStart() {
        super.onStart()
        val filterIntent = IntentFilter()
        filterIntent.addAction("REFRESH_TAGS")
        filterIntent.addAction("ACCEPT_OPTIONS")
        filterIntent.addAction("MQTT_SYNC_DATA")
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(broadcastReceiver, IntentFilter(filterIntent))
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
     * Setup recycler view
     *
     */
    private fun setupRecyclerView() {
        initRecyclerViewPayments()
        initRecyclerViewCart()
    }

    /**
     * Init recycler view payments
     *
     */
    private fun initRecyclerViewPayments() {
        if (AppSettings.Options.Payment.MasterCard) {
            listPaymentType.add(PaymentType.MASTER_CARD.value)
        }
        if (AppSettings.Options.Payment.EzLink) {
            listPaymentType.add(PaymentType.EZ_LINK.value)
        }
        if (AppSettings.Options.Payment.PayNow) {
            listPaymentType.add(PaymentType.PAY_NOW.value)
        }
        if (AppSettings.Options.Payment.Wallet) {
            listPaymentType.add(PaymentType.KONBINI_WALLET.value)
        }
        if (AppSettings.Options.Payment.Cash) {
            listPaymentType.add(PaymentType.CASH.value)
        }
        if (AppSettings.Options.MachineTypeActivated == MachineType.DISCOUNT_MODE.value) {
            listPaymentType.add(PaymentType.DISCOUNT.value)
        }

        var spanCount = listPaymentType.size
        if (spanCount > 3) spanCount = 3
        paymentAdapter = PaymentAdapter(this)
        val manager =
            GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false)
        binding.recyclerViewPayments.layoutManager = manager
        binding.recyclerViewPayments.adapter = paymentAdapter
        paymentAdapter.setItems(items = ArrayList(listPaymentType))
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
     * Setup observers
     *
     */
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                        setBlink(AlarmType.SUCCESS)
                        displayMessage(_state.message)
                        LogUtils.logInfo(_state.message)
                        AppContainer.CurrentTransaction.paymentState = PaymentState.Success
                        AppContainer.CurrentTransaction.resetTemporaryInfo()
                        // Refresh cart
                        refreshCart()

                        if (AppSettings.Options.Printer.Bluetooth || AppSettings.Options.Printer.USB) {
                            // Print Receipt
                            LogUtils.logInfo("Start Print receipt")
                            printReceipt(AppContainer.CurrentTransaction.cartLocked)
                            AudioManager.instance.soundPaymentSuccess()
                        }

                        MainApplication.startRealTimeInventory()

                        val message = getString(R.string.message_put_plate_on_the_tray)
                        resetMessage(message, 0)
                    }
                    Resource.Status.ERROR -> {
                        setBlink(AlarmType.ERROR)
                        displayMessage(_state.message)
                        AudioManager.instance.soundBuzzer()
                        AppContainer.CurrentTransaction.paymentState = PaymentState.Error

//                        val message = getString(R.string.message_please_tap_card_again)
//                        val voice = R.raw.please_tap_card_again
//                        resetMessage(message, voice)
//                        AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                        val message = getString(R.string.message_put_plate_on_the_tray)
                        resetMessage(message, 0)
                        AppContainer.CurrentTransaction.paymentState = PaymentState.Preparing
                    }
                    else -> {

                    }
                }
            }
        }

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
            if (clickedTitleModel == 3 && clickedTitleTotal == 3) {
                clickedTitleModel = 0
                clickedTitleTotal = 0
                // Goto Login
                gotoLogin()
            } else {
                if (clickedTitleModel > 3) clickedTitleModel = 0
            }
        }

        binding.rfidTotalCount.setOnClickListener {
            binding.rfidTotalCount.blink(Color.RED, 1, 50L)
            clickedTitleTotal += 1
            Log.e(TAG, "Clicked Title Total-$clickedTitleTotal")
            if (clickedTitleModel == 3 && clickedTitleTotal == 3) {
                clickedTitleModel = 0
                clickedTitleTotal = 0
                // Goto Login
                gotoLogin()
            } else {
                if (clickedTitleTotal > 3) clickedTitleTotal = 0
            }
        }

        binding.rfidCancelPayment.setSafeOnClickListener {
            cancelPayment()
        }

        // TODO: Start TrungPQ add to test
        binding.spinKitMessage.setSafeOnClickListener {
            AppContainer.CurrentTransaction.cardNFC = "8d2ed739"
            viewModel.debit()
        }

        binding.rfidMessageTitle.setSafeOnClickListener {
            AppContainer.GlobalVariable.listEPC.clear()
            AppContainer.GlobalVariable.listEPC.add("01800000020300108CCFB14E")
            AppContainer.GlobalVariable.listEPC.add("04800000020300108CCFB14E")

            AppContainer.CurrentTransaction.listEPC.clear()
            AppContainer.CurrentTransaction.listEPC.addAll(AppContainer.GlobalVariable.listEPC)

            // Get list tags
            val listTagEntity =
                AppContainer.GlobalVariable.getListTagEntity(AppContainer.GlobalVariable.listEPC)
            AppContainer.CurrentTransaction.listTagEntity = listTagEntity

            MainApplication.timeTagSizeChanged = 0L
            AppContainer.CurrentTransaction.refreshCart()

            // Add or Remove items to cart
            val intent = Intent()
            intent.action = "REFRESH_TAGS"
            LocalBroadcastManager.getInstance(MainApplication.instance.applicationContext).sendBroadcast(intent)
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
        }
//        Remove Swipe to show Overlay
//        val swipeLayoutToHideAndShow = SwipeLayoutShowHide()
//        swipeLayoutToHideAndShow.initialize(
//            binding.rfidProducts,
//            binding.rfidMaskReading,
//            Arrays.asList(
//                SwipeLayoutShowHide.SwipeDirection.rightToLeft,
//                SwipeLayoutShowHide.SwipeDirection.leftToRight,
//                SwipeLayoutShowHide.SwipeDirection.topToBottom,
//                SwipeLayoutShowHide.SwipeDirection.bottomToTop
//            ),
//            50
//        )
    }

    override fun onClickedPayment(payment: String) {
        // Check selected payment
        val paymentState = AppContainer.CurrentTransaction.paymentState
        if (paymentState == PaymentState.ReadyToPay || paymentState == PaymentState.InProgress) return
        when (payment) {
            PaymentType.MASTER_CARD.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                changeColorWhenSelectPayment()

                // Start countdown timeout and voice
                timerTimeoutPayment.start()
                AudioManager.instance.soundProcessingPayment()

                // Change Payment state
                AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                AppContainer.CurrentTransaction.paymentType = PaymentType.MASTER_CARD

                // Locked cart
                AppContainer.CurrentTransaction.cartLocked()

                // Listener MasterCard
                listenerMasterCard()

            }
            PaymentType.EZ_LINK.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                changeColorWhenSelectPayment()

                // Start countdown timeout and voice
                timerTimeoutPayment.start()
                AudioManager.instance.soundProcessingPayment()

                // Change Payment state
                AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                AppContainer.CurrentTransaction.paymentType = PaymentType.EZ_LINK

                // Locked cart
                AppContainer.CurrentTransaction.cartLocked()

                // Listener EzLink
                listenerEzLink()
            }
            PaymentType.PAY_NOW.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                changeColorWhenSelectPayment()

                // Start countdown timeout and voice
                timerTimeoutPayment.start()
                AudioManager.instance.soundProcessingPayment()

                // Change Payment state
                AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                AppContainer.CurrentTransaction.paymentType = PaymentType.PAY_NOW

                // Locked cart
                AppContainer.CurrentTransaction.cartLocked()

                // Listener PayNow
                listenerPayNow()
            }
            PaymentType.KONBINI_WALLET.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                changeColorWhenSelectPayment()

                // Start countdown timeout and voice
                timerTimeoutPayment.start()
                AudioManager.instance.soundPleaseTapCard()

                // Change Payment state
                AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                AppContainer.CurrentTransaction.paymentType = PaymentType.KONBINI_WALLET

                // Locked cart
                AppContainer.CurrentTransaction.cartLocked()
            }
            PaymentType.CASH.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                changeColorWhenSelectPayment()

                // Change Payment state
                AppContainer.CurrentTransaction.paymentState = PaymentState.Success
                AppContainer.CurrentTransaction.paymentType = PaymentType.CASH

                // Locked cart
                AppContainer.CurrentTransaction.cartLocked()

                val message = getString(R.string.message_success_payment)
                displayMessage(message)

                handlePaymentSuccess()
            }
            PaymentType.DISCOUNT.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                val paymentState = AppContainer.CurrentTransaction.paymentState
                if (paymentState != PaymentState.Init && paymentState != PaymentState.Preparing) {
                    return
                }
                AudioManager.instance.soundEnterDiscount()
                var message = getString(R.string.message_please_scan_barcode)
                if (AppSettings.Options.Discount.NFC) {
                    message = getString(R.string.message_please_tap_membership_card)
                }
                pDialog = AlertDialogUtil.showProgress(
                    message,
                    requireContext()
                )

                if (AppSettings.Options.Discount.Barcode) {
                    pDialog.setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            val pressedKey = event.unicodeChar.toChar()
                            barcode += pressedKey
                        }
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (event.keyCode) {
                                KeyEvent.KEYCODE_ENTER -> {
                                    Log.e("BARCODE_VALUE", barcode)

                                    barcode = barcode.split("\n")[0]
                                    AppContainer.CurrentTransaction.ccwId1 = barcode
                                    if (this::pDialog.isInitialized) {
                                        if (pDialog.isShowing) {
                                            pDialog.dismiss()
                                        }
                                    }
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
        }
    }

    override fun onClickedCartItem(cartEntity: CartEntity, type: ActionCart) {
        val state = AppContainer.CurrentTransaction.paymentState
        if (state == PaymentState.ReadyToPay || state == PaymentState.InProgress) {
            setBlink(AlarmType.ERROR)
            AudioManager.instance.soundDoNotChangeItem()
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
        // Handle Hardware
        if (AppContainer.CurrentTransaction.paymentType == PaymentType.MASTER_CARD
            || AppContainer.CurrentTransaction.paymentType == PaymentType.EZ_LINK
            || AppContainer.CurrentTransaction.paymentType == PaymentType.PAY_NOW
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
            AudioManager.instance.soundPaymentTimeout()
            displayMessage(getString(R.string.message_payment_timeout))
        } else {
            AudioManager.instance.soundPaymentCancelled()
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
     * Display Count Items
     */
    private fun displayCountItem() {
        val countItems = AppContainer.CurrentTransaction.countItems
        binding.rfidItemCount.text = countItems.toString()
    }

    /**
     * Display message
     *
     * @param message
     */
    private fun displayMessage(message: String, isLoading: Boolean = false) {
        binding.rfidMessageTitle.text = message
        if (AppContainer.CurrentTransaction.paymentState == PaymentState.ReadyToPay) {
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
                    AudioManager.instance.soundPleaseTapCardAgain()

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
        LogUtils.logInfo("Display Cart: ${gson.toJson(cart)}")
    }

    /**
     * Refresh cart
     *
     */
    private fun refreshCart() {
        displayCart()
        displayCountItem()
        displayTotal()
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
                binding.rfidMessageTitle.blink(Color.GREEN, 3, 500L)
                binding.rfidItemCount.blink(Color.GREEN, 3, 500L)
                binding.rfidTotalCount.blink(Color.GREEN, 3, 500L)
                binding.selectPayment.blink(Color.GREEN, 3, 500L)
            }
            AlarmType.WARNING -> {
                binding.rfidProducts.blink(Color.YELLOW, 3, 500L)
                binding.layoutMessageTitle.blink(Color.YELLOW, 3, 500L)
                binding.rfidMessageTitle.blink(Color.YELLOW, 3, 500L)
                binding.rfidItemCount.blink(Color.YELLOW, 3, 500L)
                binding.rfidTotalCount.blink(Color.YELLOW, 3, 500L)
                binding.selectPayment.blink(Color.YELLOW, 3, 500L)
            }
            AlarmType.ERROR -> {
                binding.rfidProducts.blink(Color.RED, 3, 500L)
                binding.layoutMessageTitle.blink(Color.RED, 3, 500L)
                binding.rfidMessageTitle.blink(Color.RED, 3, 500L)
                binding.rfidItemCount.blink(Color.RED, 3, 500L)
                binding.rfidTotalCount.blink(Color.RED, 3, 500L)
                binding.selectPayment.blink(Color.RED, 3, 500L)
            }
            else -> {
                binding.rfidProducts.blink(Color.GRAY, 3, 500L)
                binding.layoutMessageTitle.blink(Color.GRAY, 3, 500L)
                binding.rfidMessageTitle.blink(Color.GRAY, 3, 500L)
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
    // endregion

    // region ================ASC Reader Functions================
    /**
     * Handle payment success
     *
     */
    private fun handlePaymentSuccess() {
        setBlink(AlarmType.SUCCESS)
        AudioManager.instance.soundPaymentSuccess()
        AppContainer.CurrentTransaction.paymentState = PaymentState.Success

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
            paymentDetail = AppContainer.CurrentTransaction.paymentType!!.value,
            paymentTime = currentTime.toString(),
            paymentState = PaymentState.Success.name,
            paymentType = AppContainer.CurrentTransaction.paymentType!!.value,
            cardType = AppContainer.CurrentTransaction.paymentType!!.value,
            cardNumber = AppContainer.CurrentTransaction.cardNFC,
            approveCode = "n/a",
            note = "n/a"
        )
        transaction.dateCreated = currentTime.toString()
        viewModel.insert(transaction)

        val message = getString(R.string.message_put_plate_on_the_tray)
        resetMessage(message, 0)
        AppContainer.CurrentTransaction.resetTemporaryInfo()
        // Refresh cart
        refreshCart()

        if (AppSettings.Options.Printer.Bluetooth || AppSettings.Options.Printer.USB) {
            // Print Receipt
            LogUtils.logInfo("Start Print receipt")
            printReceipt(AppContainer.CurrentTransaction.cartLocked)
        }
    }

    /**
     * Handle payment error
     *
     * @param _message
     */
    private fun handlePaymentError(_message: String) {
        setBlink(AlarmType.ERROR)
        displayMessage(ErrorCodeIM30.handleMessageIuc(_message, requireContext()))
        AudioManager.instance.soundBuzzer()
        AppContainer.CurrentTransaction.paymentState = PaymentState.Preparing

//        val message = getString(R.string.message_please_tap_card_again)
//        val voice = R.raw.please_tap_card_again
//        resetMessage(message, voice)
//        AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
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
                IM30Interface.instance.sale(amount, {
                    // Success
                    activity?.runOnUiThread {
                        handlePaymentSuccess()
                    }
                }, { _message ->
                    // Error
                    activity?.runOnUiThread {
                        handlePaymentError(_message)
                    }
                }, { _message ->
                    // Callback
                    activity?.runOnUiThread {
                        if (_message.lowercase().contains("PLEASE WAIT".lowercase())) {
                            // Reset countdown timeout payment
                            timerTimeoutPayment.cancel()
                            timeout = AppSettings.Options.Payment.Timeout

                            AudioManager.instance.soundProcessingPayment()
                            AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
                        }
                        Log.e(TAG, _message)
                        LogUtils.logInfo(_message)
                        displayMessage(_message)
                    }
                }, {
                    // Cancel
                    activity?.runOnUiThread {}
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
                IM30Interface.instance.cpas(amount, {
                    // Success
                    activity?.runOnUiThread {
                        handlePaymentSuccess()
                    }
                }, { _message ->
                    // Error
                    activity?.runOnUiThread {
                        handlePaymentError(_message)
                    }
                }, { _message ->
                    // Callback
                    activity?.runOnUiThread {
                        if (_message.lowercase().contains("PLEASE WAIT".lowercase())) {
                            // Reset countdown timeout payment
                            timerTimeoutPayment.cancel()
                            timeout = AppSettings.Options.Payment.Timeout

                            AudioManager.instance.soundProcessingPayment()
                            AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
                        }
                        Log.e(TAG, _message)
                        LogUtils.logInfo(_message)
                        displayMessage(_message)
                    }
                }, {
                    // Cancel
                    activity?.runOnUiThread {}
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
                IM30Interface.instance.mpqr(amount, IM30Interface.MpqrWalletLabel.DBSMAX_PAYNOW, {
                    // Success
                    activity?.runOnUiThread {
                        handlePaymentSuccess()
                    }
                }, { _message ->
                    // Error
                    activity?.runOnUiThread {
                        handlePaymentError(_message)
                    }
                }, { _message ->
                    // Callback
                    activity?.runOnUiThread {
                        if (_message.lowercase().contains("PLEASE WAIT".lowercase())) {
                            // Reset countdown timeout payment
                            timerTimeoutPayment.cancel()
                            timeout = AppSettings.Options.Payment.Timeout

                            AudioManager.instance.soundProcessingPayment()
                            AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
                        }
                        Log.e(TAG, _message)
                        LogUtils.logInfo(_message)
                        displayMessage(_message)
                    }
                }, {
                    // Cancel
                    activity?.runOnUiThread {}
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
                                    && AppContainer.CurrentTransaction.paymentType == PaymentType.KONBINI_WALLET
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
                                    if (this::pDialog.isInitialized) {
                                        if (pDialog.isShowing) {
                                            pDialog.dismiss()
                                        }
                                    }
                                    AppContainer.CurrentTransaction.ccwId1 = AppContainer.CurrentTransaction.cardNFC
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
                                AppContainer.CurrentTransaction.currentDiscount = discountEntity.discountValue.toFloat()
                                AppContainer.CurrentTransaction.refreshCart()
                                refreshCart()
                                return@outForeach
                            }
                        }
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
            // Show message warning cart is empty
            displayMessage(getString(R.string.message_warning_cart_is_empty))
            setBlink(AlarmType.ERROR)
            AudioManager.instance.soundCartIsEmpty()

            val message = getString(R.string.message_put_plate_on_the_tray)
            resetMessage(message, 0)
        }
        return !validate
    }
    // endregion

    private fun printReceipt(cartLocked: MutableList<CartEntity>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val lastNumber = viewModel.getLastTransactionId()
            if (lastNumber != null) {
                orderNumber = lastNumber + 1
            }

            if (AppSettings.Options.Printer.Bluetooth) {
                // Bluetooth
                printReceiptBluetooth(cartLocked)
            } else {
                if (AppSettings.Options.Printer.TCP) {
                    // TCP
                    printReceiptTCP(cartLocked)
                } else {
                    // USB
                    printReceiptUSB()
                }
            }
        }
    }

    private fun printReceiptBluetooth(cartLocked: MutableList<CartEntity>) {
        val device = BluetoothPrintersConnections.selectFirstPaired()
        val printer =
            EscPosPrinter(device, 203, AppSettings.ReceiptPrinter.WidthPaper.toFloat(), 32)
        val content = formatReceipt(cartLocked)
        printer.printFormattedTextAndCut(content)
    }

    private fun printReceiptTCP(cartLocked: MutableList<CartEntity>) {
        val ip = AppSettings.ReceiptPrinter.TCP
        if (ip.isNotEmpty()) {
            val printer = EscPosPrinter(
                TcpConnection(ip, 9300, 15),
                203,
                AppSettings.ReceiptPrinter.WidthPaper.toFloat(),
                32
            )
            val content = formatReceipt(cartLocked)
            printer.printFormattedTextAndCut(content)
        } else {
            AlertDialogUtil.showError(
                getString(R.string.message_error_TCP_is_empty),
                requireContext()
            )
        }
    }

    private fun printReceiptUSB() {
        val usbConnection = UsbPrintersConnections.selectFirstConnected(requireContext())
        val usbManager = requireActivity().getSystemService(Context.USB_SERVICE) as UsbManager?
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
        }
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
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
                            val content =
                                formatReceipt(AppContainer.CurrentTransaction.cartLocked)
                            printer.printFormattedTextAndCut(content)
                        }
                    }
                }
            }
        }
    }

    private fun formatReceipt(cartLocked: MutableList<CartEntity>): String {
        return "[C]<font size='tall'>Store: ${AppSettings.Machine.Store}</font>\n" +
                "[C]<font size='tall'>Terminal: ${AppSettings.Machine.Terminal}</font>\n" +
                "[C]<font size='tall'>Date: ${Date()}</font>\n" +
                "[C]<u><font size='big'>RECEIPT</font></u>\n" +
                "[L]\n" +
                "[C]<u><font size='tall'>ORDER N°${"%06d".format(orderNumber)}</font></u>\n" +
                "[L]\n" +
                formatContent(cartLocked) +
                "[L]\n" +
                "[L]<font size='tall'>Membership :</font>\n" +
                "[L]Display Name: ${if (displayName.isEmpty()) "N/A" else displayName}\n" +
                "[L]Balance: ${if (balance == 0F) "N/A" else formatCurrency(balance)}\n" +
                "[L]\n" +
                "[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
                "[C]<qrcode size='20'>831254784551</qrcode>" +
                "[L]\n" +
                "[L]\n" +
                "[L]\n"
    }

    private fun formatContent(cartLocked: MutableList<CartEntity>): String {
        var total = 0F
        var items = ""
        cartLocked.forEach { cartEntity ->
            val price = formatCurrency(cartEntity.price.toFloat() * cartEntity.quantity)
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
                "[C]================================\n" +
                "[C]<font size='tall'>Tel: ${AppSettings.Company.Tel}</font>\n" +
                "[C]<font size='tall'>Email: ${AppSettings.Company.Email}</font>\n" +
                "[C]<font size='tall'>Address: ${AppSettings.Company.Address}</font>\n" +
                "[C]<font size='tall'>Thank you!!!</font>\n"
    }
}