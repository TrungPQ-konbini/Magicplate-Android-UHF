package com.konbini.magicplateuhf.ui.sales.magicPlate

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.data.entities.MenuEntity
import com.konbini.magicplateuhf.data.entities.TimeBlockEntity
import com.konbini.magicplateuhf.data.entities.TransactionEntity
import com.konbini.magicplateuhf.data.enum.PaymentState
import com.konbini.magicplateuhf.data.remote.wallet.request.DebitRequest
import com.konbini.magicplateuhf.data.remote.wallet.request.WalletTokenRequest
import com.konbini.magicplateuhf.data.remote.wallet.response.ErrorResponse
import com.konbini.magicplateuhf.data.repository.*
import com.konbini.magicplateuhf.utils.CommonUtil
import com.konbini.magicplateuhf.utils.LogUtils
import com.konbini.magicplateuhf.utils.Resource
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MagicPlateViewModel @Inject constructor(
    private val timeBlockRepository: TimeBlockRepository,
    private val menuRepository: MenuRepository,
    private val productRepository: ProductRepository,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val offlineDataRepository: OfflineDataRepository
) : ViewModel() {
    companion object {
        const val TAG = "MagicPlateViewModel"
    }

    private val gson = Gson()
    private val resources = MainApplication.shared()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    suspend fun getAllProducts() = productRepository.getAll()

    suspend fun getAllMenu() = menuRepository.getAll()

    suspend fun getAllTimeBlock() = timeBlockRepository.getAll()

    fun getCurrentTimeBock(): TimeBlockEntity? {
        val calendar = Calendar.getInstance()
        val timeFormat = "HHmm"
        val stf = SimpleDateFormat(timeFormat, Locale.getDefault())
        val currentTime = stf.format(calendar.time).toString()
        val timeBlocks = AppContainer.InitData.listTimeBlocks

        timeBlocks.forEach { _timeBlockEntity ->
            val toHour = if (_timeBlockEntity.toHour.toInt() != 0) {
                _timeBlockEntity.toHour.toInt()
            } else {
                2400
            }
            if (_timeBlockEntity.fromHour.toInt() <= currentTime.toInt() && currentTime.toInt() <= toHour) {
                return _timeBlockEntity
            }
        }

        return null
    }

    fun getMenusToday(): MutableList<MenuEntity> {
        val currentTimeBock = AppContainer.InitData.currentTimeBock ?: return mutableListOf()

        val calendar = Calendar.getInstance()
        val dateFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
        val today = sdf.format(calendar.time).toString()

        val listMenusToday: MutableList<MenuEntity> = mutableListOf()
        val listMenus = AppContainer.InitData.listMenus
        listMenus.forEach { menuEntity ->
            if (menuEntity.menuDate == today && currentTimeBock.timeBlockTitle == menuEntity.timeBlockTitle) {
                listMenusToday.add(menuEntity)
            }
        }
        return listMenusToday
    }

    fun debit() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentTime = calendar.timeInMillis
            _state.emit(
                State(
                    Resource.Status.LOADING,
                    resources.getString(R.string.message_processing)
                )
            )
            try {
                // Get token wallet
                val requestTokenWallet = WalletTokenRequest(
                    "client_credentials",
                    AppSettings.Cloud.ClientId,
                    AppSettings.Cloud.ClientSecret
                )
                val tokenWallet = async {
                    walletRepository.getAccessToken(AppSettings.Cloud.Host, requestTokenWallet)
                }

                if (tokenWallet.await().status == Resource.Status.SUCCESS) {
                    tokenWallet.await().data?.let { _walletTokenResponse ->
                        // Params
                        val macAddress = AppSettings.Machine.MacAddress
                        val source = AppSettings.Machine.Source
                        val terminal = AppSettings.Machine.Terminal
                        val store = AppSettings.Machine.Store

                        // Debit wallet
                        val requestDebit = DebitRequest(
                            _walletTokenResponse.access_token!!,
                            AppContainer.CurrentTransaction.cardNFC,
                            "ccw_id1",
                            AppContainer.CurrentTransaction.totalPrice,
                            "$source\n" +
                                    "$terminal\n" +
                                    "$store\n" +
                                    "Machine: $macAddress"
                        )
                        val debit =
                            withContext(Dispatchers.Default) {
                                walletRepository.debit(AppSettings.Cloud.Host, requestDebit)
                            }

                        if (debit.status == Resource.Status.SUCCESS) {
                            debit.data?.let { _responseDebit ->
                                // Save transaction
                                val transaction = TransactionEntity(
                                    0,
                                    uuid = UUID.randomUUID().toString(),
                                    amount = AppContainer.CurrentTransaction.totalPrice.toString(),
                                    discountPercent = "0.0",
                                    taxPercent = "0.0",
                                    buyer = _responseDebit.userRegistryId ?: "",
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

                                val msg = String.format(
                                    resources.getString(R.string.message_success_payment_balance),
                                    CommonUtil.formatCurrency(_responseDebit.balance ?: 0F)
                                )
                                insert(transaction)
                                _state.emit(
                                    State(
                                        status = Resource.Status.SUCCESS,
                                        message = msg,
                                        isFinish = !AppSettings.Options.SyncOrderRealtime
                                    )
                                )
                            }
                        } else {
                            var messageDetail = ""
                            // Debit fail
                            val message = debit.message
                            try {
                                val errorResponse =
                                    gson.fromJson(message, ErrorResponse::class.java)
                                messageDetail =
                                    "${errorResponse.errorCode} - ${errorResponse.message}"
                                _state.emit(
                                    State(
                                        Resource.Status.ERROR,
                                        "Error: $messageDetail"
                                    )
                                )
                            } catch (ex: JsonParseException) {
                                _state.emit(
                                    State(
                                        Resource.Status.ERROR,
                                        "Error: $message"
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // Get token wallet fail
                    _state.emit(
                        State(
                            Resource.Status.ERROR,
                            resources.getString(R.string.message_error_get_token)
                        )
                    )
                }
            } catch (exception: Exception) {
                exception.message?.let { Log.e(TAG, it) }
                LogUtils.logInfo(exception.message ?: "Error Occurred!")
                _state.emit(
                    State(
                        Resource.Status.ERROR,
                        exception.message ?: "Error Occurred!"
                    )
                )
            }
        }
    }

    fun insert(transactionEntity: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.insert(transactionEntity)

            if (!AppSettings.Options.SyncOrderRealtime) {
                offlineDataRepository.insert(transactionEntity)
            } else {
                val bodyRequest = CommonUtil.formatCreateAnOrderRequest(transactionEntity)
                Log.e(TAG, gson.toJson(bodyRequest))

                val createAnOrder = transactionRepository.createAnOrder(
                    url = AppSettings.Cloud.Host,
                    bodyRequest
                )

                if (createAnOrder.status == Resource.Status.SUCCESS) {
                    createAnOrder.data?.let { _createAnOrder ->
                        // Update database order number
                        updateTransaction(transactionEntity, _createAnOrder.number?.toInt() ?: 0)
                        _state.emit(
                            State(
                                status = Resource.Status.SUCCESS,
                                message = String.format(
                                    resources.getString(R.string.message_success_sync_order),
                                    _createAnOrder.number
                                ),
                                isFinish = true
                            )
                        )
                    }
                } else {
                    // Show message
                    var messageDetail = ""
                    // Create An Order fail
                    val message = createAnOrder.message
                    try {
                        val errorResponse =
                            gson.fromJson(message, ErrorResponse::class.java)
                        messageDetail =
                            "${errorResponse.errorCode}: ${errorResponse.message}"
                        _state.emit(
                            State(
                                Resource.Status.ERROR,
                                "Error: $messageDetail"
                            )
                        )
                    } catch (ex: JsonParseException) {
                        _state.emit(
                            State(
                                Resource.Status.ERROR,
                                "Error: $message"
                            )
                        )
                    }
                    // Save to sync offline data
                    offlineDataRepository.insert(transactionEntity)
                }
            }
        }
    }

    private fun updateTransaction(transactionEntity: TransactionEntity, syncId: Int) =
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentTime = calendar.timeInMillis.toString()
            transactionRepository.update(transactionEntity.uuid, syncId, currentTime)
        }
}