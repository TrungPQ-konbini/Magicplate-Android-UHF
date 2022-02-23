package com.konbini.magicplateuhf.ui.sales.magicPlate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.gson.Gson
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.CartEntity
import com.konbini.magicplateuhf.data.enum.*
import com.konbini.magicplateuhf.databinding.FragmentMagicPlateBinding
import com.konbini.magicplateuhf.utils.*
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.blink
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.convertStringToShortTime
import com.konbini.magicplateuhf.utils.CommonUtil.Companion.formatCurrency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@AndroidEntryPoint
class MagicPlateFragment : Fragment(), PaymentAdapter.ItemListener, CartAdapter.ItemListener {

    companion object {
        const val TAG = "MagicPlateFragment"
    }

    private val gson = Gson()
    private var processing = false

    private val changeTagReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "REFRESH_TAGS" -> {
                    // Refresh cart
                    refreshCart()
                }
                "ACCEPT_OPTIONS" -> {
                    // Refresh cart
                    refreshCart()
                }
            }
        }
    }

    private var clickedTitleModel = 0
    private var clickedTitleTotal = 0

    private lateinit var cartAdapter: CartAdapter
    private lateinit var paymentAdapter: PaymentAdapter
    private var listPaymentType: MutableList<String> = mutableListOf()

    private var binding: FragmentMagicPlateBinding by autoCleared()
    private val viewModel: MagicPlateViewModel by viewModels()

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
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(changeTagReceiver, IntentFilter(filterIntent))
    }

    override fun onStop() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(changeTagReceiver)
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

        val spanCount = listPaymentType.size
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
                        displayMessage(_state.message)
                        AudioManager.instance.soundProcessingPayment()
                        AppContainer.CurrentTransaction.paymentState = PaymentState.InProgress
                    }
                    Resource.Status.SUCCESS -> {
                        setBlink(AlarmType.SUCCESS)
                        displayMessage(_state.message)
                        if (!_state.isFinish) {
                            AudioManager.instance.soundPaymentSuccess()
                        }
                        AppContainer.CurrentTransaction.paymentState = PaymentState.Success

                        if (_state.isFinish) {
                            val message = getString(R.string.message_put_plate_on_the_tray)
                            resetMessage(message, 0)
                            AppContainer.CurrentTransaction.resetTemporaryInfo()
                            // Refresh cart
                            refreshCart()
                        }
                    }
                    Resource.Status.ERROR -> {
                        setBlink(AlarmType.ERROR)
                        displayMessage(_state.message)
                        AudioManager.instance.soundBuzzer()
                        AppContainer.CurrentTransaction.paymentState = PaymentState.Error

                        val message = getString(R.string.message_please_tap_card_again)
                        val voice = R.raw.please_tap_card_again
                        resetMessage(message, voice)
                        AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                    }
                    else -> {

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
        binding.rfidItemCount.setOnClickListener {
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
    }

    /**
     * Init data
     *
     */
    private fun initData() {
        viewLifecycleOwner.lifecycleScope.launch {
            showHideLoading(true)

            AppContainer.InitData.listProducts = viewModel.getAllProducts().toMutableList()
            AppContainer.InitData.listTimeBlocks = viewModel.getAllTimeBlock().toMutableList()
            AppContainer.InitData.listMenus = viewModel.getAllMenu().toMutableList()
            AppContainer.InitData.currentTimeBock = viewModel.getCurrentTimeBock()
            AppContainer.InitData.listMenusToday = viewModel.getMenusToday()

            displayTimeBlock()
            displayTotal()
            displayCountItem()
            displayMessage(getString(R.string.message_put_plate_on_the_tray))

            showHideLoading(false)
        }
    }

    override fun onClickedPayment(payment: String) {
        // Check selected payment
        val paymentState = AppContainer.CurrentTransaction.paymentState
        if (paymentState == PaymentState.ReadyToPay || paymentState == PaymentState.InProgress) return
        when (payment) {
            PaymentType.MASTER_CARD.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                // Show message and voice
                displayMessage(getString(R.string.message_processing))
                AudioManager.instance.soundProcessingPayment()

                // Change Payment state
                AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                AppContainer.CurrentTransaction.paymentType = PaymentType.MASTER_CARD

                // Locked cart
                AppContainer.CurrentTransaction.cartLocked()

                // Listener MasterCard

            }
            PaymentType.EZ_LINK.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                // Show message and voice
                displayMessage(getString(R.string.message_processing))
                AudioManager.instance.soundProcessingPayment()

                // Change Payment state
                AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                AppContainer.CurrentTransaction.paymentType = PaymentType.EZ_LINK

                // Locked cart
                AppContainer.CurrentTransaction.cartLocked()

                // Listener MasterCard
            }
            PaymentType.PAY_NOW.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                // Show message and voice
                displayMessage(getString(R.string.message_processing))
                AudioManager.instance.soundProcessingPayment()

                // Change Payment state
                AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                AppContainer.CurrentTransaction.paymentType = PaymentType.PAY_NOW

                // Locked cart
                AppContainer.CurrentTransaction.cartLocked()

                // Listener MasterCard
            }
            PaymentType.KONBINI_WALLET.value -> {
                val validate = validateSelectPayment()
                if (!validate) return

                // Show message and voice
                displayMessage(getString(R.string.message_please_tap_card))
                AudioManager.instance.soundPleaseTapCard()

                // Change Payment state
                AppContainer.CurrentTransaction.paymentState = PaymentState.ReadyToPay
                AppContainer.CurrentTransaction.paymentType = PaymentType.KONBINI_WALLET

                // Locked cart
                AppContainer.CurrentTransaction.cartLocked()
            }
        }
    }

    override fun onClickedCartItem(cartEntity: CartEntity, type: ActionCart) {
        val state = AppContainer.CurrentTransaction.paymentState
        if (state == PaymentState.ReadyToPay || state == PaymentState.InProgress) {
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
                val dialog = ModifiersDialog(cartEntity, MachineType.MAGIC_PLATE.value)
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
        processing = show
    }

    /**
     * Display current TimeBlock
     */
    private fun displayTimeBlock() {
        val currentTimeBock = AppContainer.InitData.currentTimeBock
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
    private fun displayMessage(message: String) {
        binding.rfidMessageTitle.text = message
    }

    /**
     * Reset message
     *
     * @param message
     * @param voice
     */
    private fun resetMessage(message: String, voice: Int) {
        object : CountDownTimer(1500, 500) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                displayMessage(message)
                when (voice) {
                    R.raw.please_tap_card_again -> {
                        AudioManager.instance.soundPleaseTapCardAgain()
                    }
                }
                cancel()
            }
        }.start()
    }

    /**
     * Display cart
     *
     */
    private fun displayCart() {
        val cart = AppContainer.CurrentTransaction.cart
        cartAdapter.setItems(ArrayList(cart))
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
                binding.rfidMessageTitle.blink(Color.GREEN, 3, 500L)
                binding.rfidItemCount.blink(Color.GREEN, 3, 500L)
                binding.rfidTotalCount.blink(Color.GREEN, 3, 500L)
                binding.selectPayment.blink(Color.GREEN, 3, 500L)
            }
            AlarmType.WARNING -> {
                binding.rfidProducts.blink(Color.YELLOW, 3, 500L)
                binding.rfidMessageTitle.blink(Color.YELLOW, 3, 500L)
                binding.rfidItemCount.blink(Color.YELLOW, 3, 500L)
                binding.rfidTotalCount.blink(Color.YELLOW, 3, 500L)
                binding.selectPayment.blink(Color.YELLOW, 3, 500L)
            }
            AlarmType.ERROR -> {
                binding.rfidProducts.blink(Color.RED, 3, 500L)
                binding.rfidMessageTitle.blink(Color.RED, 3, 500L)
                binding.rfidItemCount.blink(Color.RED, 3, 500L)
                binding.rfidTotalCount.blink(Color.RED, 3, 500L)
                binding.selectPayment.blink(Color.RED, 3, 500L)
            }
            else -> {
                binding.rfidProducts.blink(Color.GRAY, 3, 500L)
                binding.rfidMessageTitle.blink(Color.GRAY, 3, 500L)
                binding.rfidItemCount.blink(Color.GRAY, 3, 500L)
                binding.rfidTotalCount.blink(Color.GRAY, 3, 500L)
                binding.selectPayment.blink(Color.GRAY, 3, 500L)
            }
        }
    }
    // endregion

    // region ================ASC Reader Functions================
    /**
     * Listener master card
     *
     */
    private fun listenerMasterCard() {
        val amount = (AppContainer.CurrentTransaction.totalPrice.toDouble() * 100).roundToInt()
        viewLifecycleOwner.lifecycleScope.launch {
            try {

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
            MainApplication.mReader.setOnStateChangeListener { slotNum, prevState, currState ->
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
                            MainApplication.mReader.power(slotNum, Reader.CARD_WARM_RESET)
                            MainApplication.mReader.setProtocol(
                                slotNum,
                                Reader.PROTOCOL_T0 or Reader.PROTOCOL_T1
                            )

                            // Send the command to the reader
                            var responseLength = MainApplication.mReader.transmit(
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
                                LogUtils.logInfo("Card NFC: $uid")
                                if (AppContainer.CurrentTransaction.paymentState == PaymentState.ReadyToPay
                                    && AppContainer.CurrentTransaction.paymentType == PaymentType.KONBINI_WALLET
                                ) {
                                    AppContainer.CurrentTransaction.paymentState =
                                        PaymentState.InProgress
                                    viewModel.debit()
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
        }
        return !validate
    }
    // endregion
}