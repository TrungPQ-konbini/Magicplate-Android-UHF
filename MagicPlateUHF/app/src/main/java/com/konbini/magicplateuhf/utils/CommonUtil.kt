package com.konbini.magicplateuhf.utils

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import com.google.gson.Gson
import com.konbini.magicplateuhf.AppContainer
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.data.entities.CartEntity
import com.konbini.magicplateuhf.data.entities.TagEntity
import com.konbini.magicplateuhf.data.entities.TransactionEntity
import com.konbini.magicplateuhf.data.enum.PaymentModeType
import com.konbini.magicplateuhf.data.remote.product.response.Option
import com.konbini.magicplateuhf.data.remote.transaction.request.*
import com.konbini.magicplateuhf.ui.SalesActivity
import java.sql.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


class CommonUtil {
    companion object {
        fun getDateJob(isNextDay: Boolean = false, hours: Int, minutes: Int): Date {
            // get Unix for now
            val calendar = Calendar.getInstance()
            val currentLocalTime = calendar.timeInMillis

            // get Unix for job
            val calendarJob = Calendar.getInstance()
            calendarJob.set(Calendar.HOUR_OF_DAY, hours)
            calendarJob.set(Calendar.MINUTE, minutes)
            calendarJob.set(Calendar.SECOND, 0)
            var jobLocalTime = calendarJob.timeInMillis

            val dateJob: Date
            if (!isNextDay) {
                if (currentLocalTime <= jobLocalTime) {
                    // Today's Job
                    dateJob = Date(jobLocalTime)
                } else {
                    // Tomorrow's Job
                    calendarJob.set(
                        Calendar.DAY_OF_MONTH,
                        calendarJob.get(Calendar.DAY_OF_MONTH) + 1
                    )
                    jobLocalTime = calendarJob.timeInMillis
                    dateJob = Date(jobLocalTime)
                }
            } else {
                // Tomorrow's Job
                calendarJob.set(Calendar.DAY_OF_MONTH, calendarJob.get(Calendar.DAY_OF_MONTH) + 1)
                jobLocalTime = calendarJob.timeInMillis
                dateJob = Date(jobLocalTime)
            }

            return dateJob
        }

        fun isNumber(s: String?): Boolean {
            return if (s.isNullOrEmpty()) false else s.all { Character.isDigit(it) }
        }

        fun theMonth(month: Int): String {
            val monthNames = arrayOf(
                "January",
                "February",
                "March",
                "April",
                "May",
                "June",
                "July",
                "August",
                "September",
                "October",
                "November",
                "December"
            )
            return monthNames[month]
        }

        fun formatCurrency(value: Float, title: String = ""): String {
            var currency = 0F
            if (value > 0) currency = value
            val format: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            if (title.isNotEmpty()) {
                return title + format.format(currency)
            }
            return format.format(currency)
        }

        fun convertStringToShortTime(shortTime: String): String {
            var time = shortTime
            if (time.length <= 3) {
                when (time.length) {
                    1 -> time = "000$time"
                    2 -> time = "00$time"
                    3 -> time = "0$time"
                }
            }
            if (time.isNotEmpty())
                time = time.substring(0, 2) + ":" + time.substring(2, 4)
            return time
        }

        fun atStartOfDay(timeInMillis: Long = 0L): Long {
            val calendar = Calendar.getInstance()
            if (timeInMillis == 0L) {
                calendar.time = Date()
            } else {
                calendar.timeInMillis = timeInMillis
            }
            calendar[Calendar.HOUR_OF_DAY] = 0
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
            return calendar.timeInMillis
        }

        fun atEndOfDay(timeInMillis: Long = 0L): Long {
            val calendar = Calendar.getInstance()
            if (timeInMillis == 0L) {
                calendar.time = Date()
            } else {
                calendar.timeInMillis = timeInMillis
            }
            calendar[Calendar.HOUR_OF_DAY] = 23
            calendar[Calendar.MINUTE] = 59
            calendar[Calendar.SECOND] = 59
            calendar[Calendar.MILLISECOND] = 999
            return calendar.timeInMillis
        }

        fun atTimeOfDay(hour: Int): Long {
            val calendar = Calendar.getInstance()
            calendar[Calendar.HOUR_OF_DAY] = hour
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            return calendar.timeInMillis
        }

        fun convertStringToMillis(date: String, format: String = "dd/M/yyyy"): Long {
            val calendar = Calendar.getInstance()
            val date = SimpleDateFormat(format, Locale.getDefault()).parse(date)
            return date.time
        }

        fun convertMillisToString(millis: Long, format: String = "dd/M/yyyy"): String {
            val stamp = Timestamp(millis)
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            val date = Date(stamp.time)
            return sdf.format(date.time).toString()
        }

        fun getGreetingMessage(): String {
            val c = Calendar.getInstance()
            return when (c.get(Calendar.HOUR_OF_DAY)) {
                in 0..11 -> "Good Morning"
                in 12..15 -> "Good Afternoon"
                in 16..20 -> "Good Evening"
                in 21..23 -> "Good Night"
                else -> "Welcome"
            }
        }

        fun formatSubmitTransactionRequest(transactionEntity: TransactionEntity): SubmitTransactionRequest {
            val formatterTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val listProducts: MutableList<SubmitTransactionRequestProducts> = mutableListOf()

            if (transactionEntity.details != PaymentModeType.TOP_UP.value) {
                // get old cart
                val cart =
                    Gson().fromJson(transactionEntity.details, Array<CartEntity>::class.java)
                        .asList()

                cart.forEach { _cartEntity ->
                    val product = SubmitTransactionRequestProducts(
                        productId = _cartEntity.productId.toInt(),
                        productQuantity = _cartEntity.quantity,
                        isCustomPrice = true,
                        customPrice = if (_cartEntity.salePrice.isEmpty()) _cartEntity.price.toDouble() else _cartEntity.salePrice.toDouble()
                    )
                    listProducts.add(product)
                }
            }

            return SubmitTransactionRequest(
                accessToken = AppContainer.GlobalVariable.currentToken,
                macAddress = AppSettings.Machine.MacAddress,
                txnDateTime = formatterTime.format(Date(transactionEntity.dateCreated.toLong())),
                txnUniqueId = transactionEntity.uuid,
                paymentType = if (transactionEntity.paymentType == PaymentModeType.KONBINI_WALLET.value) "KONBI_WALLET" else transactionEntity.paymentType,
                cardNumber = transactionEntity.cardNumber,
                orderStatus = AppSettings.Cloud.OrderStatus,
                source = AppSettings.Machine.Source,
                terminal = AppSettings.Machine.Terminal,
                store = AppSettings.Machine.Store,
                others = "",
                slotId = "",
                discountType = "0",
                ccwId1 = "",
                ccwId2 = "",
                ccwId3 = "",
                products = ArrayList(listProducts)
            )
        }

        fun formatCreateAnOrderRequest(transactionEntity: TransactionEntity): OrderRequest {
            // get old cart
            val cart =
                Gson().fromJson(transactionEntity.details, Array<CartEntity>::class.java).asList()

            // format meta data
            val metaData = formatMetaData(transactionEntity)

            // format line items
            val lineItems = formatLineItems(cart = cart)

            return OrderRequest(
                paymentMethod = transactionEntity.paymentType,
                paymentMethodTitle = transactionEntity.paymentType,
                status = AppSettings.Cloud.OrderStatus,
                setPaid = true,
                customerId = if (transactionEntity.buyer != null && transactionEntity.buyer != "n/a") transactionEntity.buyer?.toInt() else null,
                metaData = ArrayList(metaData),
                lineItems = ArrayList(lineItems)
            )
        }

        private fun formatMetaData(transactionEntity: TransactionEntity): MutableList<MetaDataOrder> {
            val metaData: MutableList<MetaDataOrder> = mutableListOf()
            val meta = MetaDataOrder()
            for (index in 1..7) {
                when (index) {
                    1 -> {
                        val meta = MetaDataOrder()
                        meta.key = "_order_source"
                        meta.value = AppSettings.Machine.Source
                        metaData.add(meta)
                    }
                    2 -> {
                        val meta = MetaDataOrder()
                        meta.key = "_order_terminal"
                        meta.value = AppSettings.Machine.Terminal
                        metaData.add(meta)
                    }
                    3 -> {
                        val meta = MetaDataOrder()
                        meta.key = "_order_store"
                        meta.value = AppSettings.Machine.Store
                        metaData.add(meta)
                    }
                    4 -> {
                        val meta = MetaDataOrder()
                        val paymentType = AppContainer.CurrentTransaction.paymentModeType
                        meta.key = "_payment_method"
                        meta.value =
                            if (paymentType == PaymentModeType.KONBINI_WALLET) PaymentModeType.KONBINI_WALLET.value
                                .lowercase() else paymentType?.value.toString()
                        metaData.add(meta)
                    }
                    5 -> {
                        val meta = MetaDataOrder()
                        meta.key = "_payment_method_title"
                        meta.value = transactionEntity.cardType
                        metaData.add(meta)
                    }
                    6 -> {
                        val meta = MetaDataOrder()
                        meta.key = "_card_number"
                        meta.value = transactionEntity.cardNumber
                        metaData.add(meta)
                    }
                    7 -> {
                        val meta = MetaDataOrder()
                        meta.key = "_mac_address"
                        meta.value = AppSettings.Machine.MacAddress
                        metaData.add(meta)
                    }
                }
            }
            return metaData
        }

        private fun formatLineItems(cart: List<CartEntity>): MutableList<LineItem> {
            val lineItems: MutableList<LineItem> = mutableListOf()
            cart.forEach { _cartEntity ->
                var total = _cartEntity.price?.toDouble()?.times(_cartEntity.quantity!!)
                if (_cartEntity.salePrice != "0") {
                    total = _cartEntity.salePrice?.toDouble()?.times(_cartEntity.quantity!!)
                }
                val metaData: MutableList<Any> = mutableListOf()
                var metaDataOptions: MetaDataOption

                if (!_cartEntity.options.isNullOrEmpty()) {
                    val options =
                        Gson().fromJson(_cartEntity.options, Array<Option>::class.java).asList()
                    options.forEach { _option ->
                        val listExOptions: MutableList<ExOptions> = mutableListOf()
                        _option.options?.forEach { _optionItem ->
                            if (total != null) {
                                if (_optionItem.isChecked) {
                                    val exOptions = ExOptions(
                                        name = if (_optionItem.name.isNullOrEmpty()) "N/A" else _option.name,
                                        value = if (_optionItem.name.isNullOrEmpty()) "N/A" else _optionItem.name,
                                        typeOfPrice = "",
                                        price = if (_optionItem.price.isNullOrEmpty()) 0.00 else _optionItem.price.toDouble(),
                                        _type = if (_optionItem.type.isNullOrEmpty()) "" else _optionItem.type,
                                    )
                                    listExOptions.add(exOptions)

                                    val metaDataItem = MetaData(
                                        key = if (_optionItem.name.isNullOrEmpty()) "N/A" else _option.name,
                                        value = if (_optionItem.name.isNullOrEmpty()) "N/A" else _optionItem.name,
                                        displayKey = if (_optionItem.name.isNullOrEmpty()) "N/A" else _optionItem.name,
                                        displayValue = if (_optionItem.name.isNullOrEmpty()) "N/A" else _optionItem.name,
                                    )
                                    metaData.add(metaDataItem)

                                    total += if (_optionItem.price.isNullOrEmpty()) 0.00 else _optionItem.price.toDouble() * _cartEntity.quantity!!
                                }
                            }
                        }
                        metaDataOptions = MetaDataOption(
                            key = "_exoptions",
                            value = ArrayList(listExOptions)
                        )
                        metaData.add(metaDataOptions)
                    }
                }

                val lineItem = LineItem(
                    productId = _cartEntity.productId?.toInt(),
                    quantity = _cartEntity.quantity,
                    subtotal = total.toString(),
                    total = total.toString(),
                    subtotalTax = "0.00",
                    totalTax = "0.00",
                    metaData = ArrayList(metaData)
                )

                lineItems.add(lineItem)
            }

            return lineItems
        }

        fun convertEpcToTagEntity(strEPC: String): TagEntity? {
            if (strEPC.length != AppSettings.Machine.LengthEPC) return null
            var customPrice = "000000"
            customPrice = strEPC.substring(18).toInt(16).toString()

            return TagEntity(
                strEPC = strEPC,
                plateModel = strEPC.substring(0, 2).toInt(16).toString(),
                serialNumber = strEPC.substring(4, 10).toInt(16).toString(),
                timestamp = strEPC.substring(10, 18).toInt(16).toString(),
                customPrice = customPrice
            )
        }

        @SuppressLint("WrongConstant")
        fun View.blink(
            color: Int,
            repeatCount: Int,
            duration: Long
        ) {
            val animator =
                ObjectAnimator.ofInt(this, "backgroundColor", Color.WHITE, color, Color.WHITE)

            animator.duration = duration
            animator.repeatMode = Animation.REVERSE
            animator.repeatCount = repeatCount
            animator.setEvaluator(ArgbEvaluator())

            animator.start()
        }

        /**
         * Restart or turn off application
         * 0: Restart App
         * 1: TurnOff App
         *
         * @param type
         *
         */
        fun restartOrTurnOffApplication(type: Int) {
            when (type) {
                0 -> { // Restart App
                    restartApp()
                }
                1 -> { // TurnOff App
                    turnOffApp()
                }
            }
        }

        private const val MAGICAL_NUMBER = 16111987

        private fun restartApp() {
            val intent = Intent(
                MainApplication.instance.applicationContext,
                SalesActivity::class.java
            )
            val mPendingIntentId: Int = MAGICAL_NUMBER
            val mPendingIntent = PendingIntent.getActivity(
                MainApplication.instance.applicationContext,
                mPendingIntentId,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            val mgr = MainApplication.instance.applicationContext
                .getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr[AlarmManager.RTC, System.currentTimeMillis() + 500] = mPendingIntent
            LogUtils.logInfo("Restart app !!!")
            exitProcess(0)
        }

        private fun turnOffApp() {
            LogUtils.logInfo("Shutdown app !!!")
            exitProcess(0)
        }

        fun checkKeyCodeExists(pressedKey: String, currentKeyCode: String): Boolean {
            var isCorrect = false
            when (pressedKey) {
                "KEYCODE_NUM_LOCK" -> {
                    if (currentKeyCode == "KEYCODE_NUM_LOCK") isCorrect = true
                }
                "KEYCODE_NUMPAD_0" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_0") isCorrect = true
                }
                "KEYCODE_NUMPAD_1" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_1") isCorrect = true
                }
                "KEYCODE_NUMPAD_2" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_2") isCorrect = true
                }
                "KEYCODE_NUMPAD_3" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_3") isCorrect = true
                }
                "KEYCODE_NUMPAD_4" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_4") isCorrect = true
                }
                "KEYCODE_NUMPAD_5" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_5") isCorrect = true
                }
                "KEYCODE_NUMPAD_6" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_6") isCorrect = true
                }
                "KEYCODE_NUMPAD_7" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_7") isCorrect = true
                }
                "KEYCODE_NUMPAD_8" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_8") isCorrect = true
                }
                "KEYCODE_NUMPAD_9" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_9") isCorrect = true
                }
                "KEYCODE_NUMPAD_DIVIDE" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_DIVIDE") isCorrect = true
                }
                "KEYCODE_NUMPAD_MULTIPLY" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_MULTIPLY") isCorrect = true
                }
                "KEYCODE_NUMPAD_SUBTRACT" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_SUBTRACT") isCorrect = true
                }
                "KEYCODE_NUMPAD_ADD" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_ADD") isCorrect = true
                }
                "KEYCODE_NUMPAD_DOT" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_DOT") isCorrect = true
                }
                "KEYCODE_NUMPAD_COMMA" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_COMMA") isCorrect = true
                }
                "KEYCODE_NUMPAD_ENTER" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_ENTER") isCorrect = true
                }
                "KEYCODE_NUMPAD_EQUALS" -> {
                    if (currentKeyCode == "KEYCODE_NUMPAD_EQUALS") isCorrect = true
                }
            }

            return isCorrect
        }

        fun slideUp(view: View) {
            view.visibility = View.VISIBLE
            val animate = TranslateAnimation(
                0f,  // fromXDelta
                0f,  // toXDelta
                view.height.toFloat(),  // fromYDelta
                0f
            ) // toYDelta
            animate.duration = 500
            animate.fillAfter = true
            view.startAnimation(animate)
        }

        fun slideDown(view: View) {
            val animate = TranslateAnimation(
                0f,  // fromXDelta
                0f,  // toXDelta
                0f,  // fromYDelta
                view.height.toFloat()
            ) // toYDelta
            animate.duration = 500
            animate.fillAfter = true
            view.startAnimation(animate)
        }

        fun slideLeft(view: View) {
            view.visibility = View.VISIBLE
            val animate = TranslateAnimation(
                0f,  // fromXDelta
                view.width.toFloat(),  // toXDelta
                0f,  // fromYDelta
                0f
            ) // toYDelta
            animate.duration = 500
            animate.fillAfter = true
            view.startAnimation(animate)
        }

        fun slideRight(view: View) {
            val animate = TranslateAnimation(
                view.width.toFloat(),  // fromXDelta
                0f,  // toXDelta
                0f,  // fromYDelta
                0f
            ) // toYDelta
            animate.duration = 500
            animate.fillAfter = true
            view.startAnimation(animate)
        }
    }
}
