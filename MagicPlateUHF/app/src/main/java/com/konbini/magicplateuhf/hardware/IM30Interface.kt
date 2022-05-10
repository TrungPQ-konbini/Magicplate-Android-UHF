package com.konbini.magicplateuhf.hardware

import android.content.Context
import android.util.Log
import com.konbini.magicplateuhf.utils.ByteUtil
//import com.konbini.serialport.implement.ISerialPortDataReceiveHandler
//import com.konbini.serialport.implement.ISerialPortLogger
//import com.konbini.serialport.implement.SerialPortInterface
import kotlinx.coroutines.*
import java.util.*
import kotlin.experimental.xor

class IM30Interface {
    private var context: Context
    var port: SerialPortInterface

    companion object {
        const val TAG = "IM30Interface"
        lateinit var instance: IM30Interface

        var CmdIdentity: Int = 0
        var CMD_TIME_OUT = 60
        val responseQueue: Queue<ByteArray> = LinkedList()
    }

    var isInit: Boolean = false
    private var dataOnSession: MutableList<Byte> = mutableListOf<Byte>()
    private var sendCommandScope: Deferred<Boolean>? = null
    private var logger: ((String) -> Unit)? = null

    // Response from terminal
    private var onTerminalResponse: ((String) -> Unit)? = null
    private var onTerminalCallback: ((String) -> Unit)? = null
    private var onSaleApproved: ((SaleResponse) -> Unit)? = null
    private var onSaleCancel: (() -> Unit)? = null
    private var onSaleError: ((String) -> Unit)? = null

    init {
        instance = this

        // Init Serial Port
        SerialPortInterface()

        port = SerialPortInterface.instance

        // Init-ed
        isInit = true
    }

    constructor(context: Context) {
        this.context = context

        port.dataReceiveHandler = DataReceiveHandler(context)
    }

    fun open(portName: String): Boolean {
        return try {
            log("Opening port: $portName")
            val result = port.open(portName, 9600)

            if (result) {
                port.startReadData()
                port.dataReceiveHandler = DataReceiveHandler(context)
//                port.onDataReceive = { data, size ->
//
//                }
                true
            } else {
                false
            }
        } catch (ex: Exception) {
            false
        }
    }

    @JvmInline
    value class DataReceiveHandler(val context: Context) : ISerialPortDataReceiveHandler {
        override fun onDataReceive(received: ByteArray, size: Int) {
            instance.processingData(received, size)
//            val hexStr: String = com.konbini.util.ByteUtil.bytes2HexStr(received, 0, size)
//            Log.d(SerialPortInterface.TAG, "<--- $hexStr")
//
//
//            if (responseQueue.isNotEmpty()) {
//                //Log.d(SerialPortInterface.TAG, "Response queue ${responseQueue.element()}")
//                if (responseQueue.element().count() > 10) {
//                    responseQueue.clear()
//                }
//            }
//
//            responseQueue.add(received)
            // Log.d(SerialPortInterface.TAG, "Response queue ${responseQueue.element()}")
        }
    }

    fun setLogger(logger: (String) -> Unit) {
        this.logger = logger
        port.logger = LoggerHandler(logger)
    }

    @JvmInline
    value class LoggerHandler(private val logger: (String) -> Unit) : ISerialPortLogger {
        override fun log(data: String) {
            logger.invoke(data)
        }

        override fun log(exception: java.lang.Exception) {
            logger.invoke(exception.toString())
        }
    }

    fun setCommandTimeout(timeout: Int) {
        CMD_TIME_OUT = timeout
    }

    suspend fun sale(
        amount: Int,
        onSaleApproved: ((SaleResponse) -> Unit)?,
        onSaleError: ((String) -> Unit)?,
        onTerminalCallBack: ((String) -> Unit)?,
        onSaleCancel: (() -> Unit)?,

        ) {

        val isCompleted = sendCommandScope?.isCompleted
        log(isCompleted.toString())
        this.onTerminalCallback = onTerminalCallBack
        this.onSaleApproved = onSaleApproved
        this.onSaleError = onSaleError
        this.onSaleCancel = onSaleCancel
        sendCommand(buildSaleCommand(amount))
    }

    suspend fun mpqr(
        amount: Int,
        walletLabel: MpqrWalletLabel,
        onSaleApproved: ((SaleResponse) -> Unit)?,
        onSaleError: ((String) -> Unit)?,
        onTerminalCallBack: ((String) -> Unit)?,
        onSaleCancel: (() -> Unit)?,
    ) {
        mpqr(
            amount,
            walletLabel.value,
            onSaleApproved,
            onSaleError,
            onTerminalCallBack,
            onSaleCancel
        )
    }

    suspend fun mpqr(
        amount: Int,
        walletLabel: String,
        onSaleApproved: ((SaleResponse) -> Unit)?,
        onSaleError: ((String) -> Unit)?,
        onTerminalCallBack: ((String) -> Unit)?,
        onSaleCancel: (() -> Unit)?,
    ) {
        val isCompleted = sendCommandScope?.isCompleted
        log(isCompleted.toString())
        this.onTerminalCallback = onTerminalCallBack
        this.onSaleApproved = onSaleApproved
        this.onSaleError = onSaleError
        this.onSaleCancel = onSaleCancel
        sendCommand(buildMpqrCommand(amount, walletLabel))
    }

    suspend fun cpas(
        amount: Int,
        onSaleApproved: ((SaleResponse) -> Unit)?,
        onSaleError: ((String) -> Unit)?,
        onTerminalCallBack: ((String) -> Unit)?,
        onSaleCancel: (() -> Unit)?,
    ) {
        this.onTerminalCallback = onTerminalCallBack
        this.onSaleError = onSaleError
        this.onSaleApproved = onSaleApproved
        this.onSaleCancel = onSaleCancel
        sendCommand(builCpasCommand(amount))
    }

    suspend fun preauth(
        amount: Int,
        onSaleApproved: ((SaleResponse) -> Unit)?,
        onSaleError: ((String) -> Unit)?,
        onTerminalCallBack: ((String) -> Unit)?,
        onSaleCancel: (() -> Unit)?,
    ): Boolean {
        this.onTerminalCallback = onTerminalCallBack
        this.onSaleError = onSaleError
        this.onSaleApproved = onSaleApproved
        this.onSaleCancel = onSaleCancel
        return sendCommand(buildPreauthEcrpeCommand(amount))
    }

    suspend fun sale(
        amount: Int,
        rrn: String,
        onSaleApproved: ((SaleResponse) -> Unit)?,
        onSaleError: ((String) -> Unit)?,
        onTerminalCallBack: ((String) -> Unit)?,
        onSaleCancel: (() -> Unit)?,
    ) {
        this.onTerminalCallback = onTerminalCallBack
        this.onSaleError = onSaleError
        this.onSaleApproved = onSaleApproved
        this.onSaleCancel = onSaleCancel
        sendCommand(buildSaleEcrpeCommand(amount, rrn))
    }

    suspend fun cancelPreauth(
        amount: Int,
        rrn: String,
        onSaleApproved: ((SaleResponse) -> Unit)?,
        onSaleError: ((String) -> Unit)?,
        onTerminalCallBack: ((String) -> Unit)?,
        onSaleCancel: (() -> Unit)?,
    ): Boolean {
        this.onTerminalCallback = onTerminalCallBack
        this.onSaleError = onSaleError
        this.onSaleApproved = onSaleApproved
        this.onSaleCancel = onSaleCancel
        return sendCommand(buildPreauthCancelEcrpeCommand(amount, rrn))
    }

    suspend fun preauthCapture(
        amount: Int,
        rrn: String,
        onSaleApproved: ((SaleResponse) -> Unit)?,
        onSaleError: ((String) -> Unit)?,
        onTerminalCallBack: ((String) -> Unit)?,
        onSaleCancel: (() -> Unit)?,
    ) {
        this.onTerminalCallback = onTerminalCallBack
        this.onSaleError = onSaleError
        this.onSaleApproved = onSaleApproved
        this.onSaleCancel = onSaleCancel
        sendCommand(buildPreauthCaptureEcrpeCommand(amount, rrn))
    }

    suspend fun settlement() {
        sendCommand(buildSettlementCommand())
    }

    fun cancel() {
        //sendCommandScope?.cancel()
        isFinished = true
        write(0x18.toByte())
    }

    fun killCurrentCommand() {
        sendCommandScope?.cancel()
    }

    @DelicateCoroutinesApi
    private suspend fun sendCommand(mainCommand: ByteArray): Boolean {
        val isRunning = sendCommandScope?.isActive ?: false
        log("Sending command job is Running: $isRunning")
        if (isRunning) {
            //sendCommandScope?.cancel()
            log("Kill old Sending job first then send command again")
            return false
        }
        sendCommandScope = GlobalScope.async {

            //1. Send EQN
            write(0x05.toByte())

            //2. Wait for ACK
            var gotAck = waitForAck()
            if (!gotAck) {
                // Resend command
                log("Failed to got ACK, try to resend command")
                return@async false
            }

            // 3. Send main command
            write(mainCommand)

            // 4. Wait for ack
            gotAck = waitForAck()
            if (gotAck) {
                write(0x04.toByte())

                val mainCmdString = ByteUtil.bytes2Ascii(mainCommand)
                val expectResponse = mainCmdString.replace("C", "R").take(5)
                // 5. Process response
                val response = processResponsePhase(expectResponse)

                if (response != "TIMEOUT") {
                    // Parsing command
                    parsingCommand(response)
                }

            } else {
                log("Failed to got ACK, try to resend command")
                return@async false
            }
            return@async true
        }

        return sendCommandScope!!.await()
    }

    public fun processingData(data: ByteArray, size: Int) {

        if (responseQueue.isNotEmpty()) {
            //Log.d(SerialPortInterface.TAG, "Response queue ${responseQueue.element()}")
            if (responseQueue.element().count() > 20) {
                responseQueue.clear()
                dataOnSession.clear()
            }
        }
        val received = data.take(size)

        log("<--- ${ByteUtil.bytes2HexStr(received.toByteArray())}")
        dataOnSession.addAll(received.toList())

        if (dataOnSession.count() > 0) {
            if (dataOnSession[0] == 0x02.toByte()) {
                var endIndex = dataOnSession.indexOf(0x03.toByte())

                // Command final ready
                if (endIndex > 0) {
                    val sessionLength = dataOnSession.count()

                    log("endIndex: $endIndex | Session Length: $sessionLength")

                    if (sessionLength >= endIndex + 2) {
                        val cmd = dataOnSession.take(endIndex + 2)

                        //log("Queue response command: " + ByteUtil.bytes2HexStr(cmd.toByteArray()))
                        //log("Data session count: " + dataOnSession.count())
                        responseQueue.add(cmd.toByteArray())
                        log("<--- ${ByteUtil.bytes2Ascii(cmd.toByteArray())}")

                        if (sessionLength == endIndex + 2) {
                            dataOnSession.clear()
                        } else if (sessionLength > endIndex + 2) {
                            // more data
                            dataOnSession.removeAll(cmd)
                            log("Data after remove: " + ByteUtil.bytes2HexStr(dataOnSession.toByteArray()))
                        }
                    } else {
                        log("Data is not enough, wait for next response")
                    }
                }
            } else {
                if (dataOnSession[0] == 0x04.toByte() || dataOnSession[0] == 0x05.toByte()
                    || dataOnSession[0] == 0x06.toByte() || dataOnSession[0] == 0x15.toByte()
                ) {
                    // log("Queue control command: " + ByteUtil.bytes2HexStr(dataOnSession.toByteArray()))
                    responseQueue.add(dataOnSession.toByteArray())
                    dataOnSession.clear()
                }
            }
        }
    }

    private fun getLatestResponse(): ByteArray {
        //log("responseQueue: " + responseQueue.count())
        return if (responseQueue.isNotEmpty()) {
            responseQueue.poll()
        } else {
            ByteArray(0)
        }
    }

    private fun clearResponseQueue() {
        responseQueue.clear()
    }

    private suspend fun waitForAck(): Boolean {

        var t = 0;
        while (true) {
            delay(100)
            t += 100

            val latestResponse = getLatestResponse()
            if (latestResponse.count() > 0) {
                val b = latestResponse[0]
                if (b == 0x06.toByte()) {
                    break
                } else {
                    continue
                }
            }


            if (t > 15000) {
                // Timeout
                return false
            }
        }
        return true
    }

    var isFinished = false

    private suspend fun processResponsePhase(expectResponse: String): String {
        isFinished = false

        var t: Long = 0
        val d: Long = 100
        var responseInString = ""
        while (true) {
            delay(d)
            val latestResponse = getLatestResponse()
            if (latestResponse.count() > 0) {
                log("POLL: " + ByteUtil.bytes2HexStr(latestResponse))
                if (latestResponse.size == 1) {
                    // Control command
                    if (latestResponse[0] == 0x05.toByte()) {
                        write(0x06.toByte())
                    }
                    // Control command
                    if (latestResponse[0] == 0x04.toByte()) {
                        if (isFinished) {
                            log("===============================End Command===============================")

                            return responseInString
                        }
                    }
                } else {
                    if (latestResponse[0] == 0x02.toByte()) {
                        write(0x06.toByte())
                        responseInString = ByteUtil.bytes2Ascii(latestResponse)

                        if (responseInString.drop(1).startsWith("R923")) {
                            t = 0;
                            val callBackMessage = getTagValue(responseInString, "34")
                            onTerminalCallback?.invoke(callBackMessage.drop(2))
                        } else {
                            if (responseInString.contains(expectResponse)) {
                                isFinished = true
                            }
                        }
                    }
                }
            }

            t += d
            if (t >= 90000) {
                // timeout
                log("Command timeout to response")
                return "TIMEOUT"
            }
        }
    }

    private fun parsingCommand(response: String) {
        //var response = cmd.drop(1).dropLast(1)

        val responseCode = getTagValue(response, "39")
        if (responseCode == "00") {
            // Command ok
            if (response.contains("R200")
                || response.contains("R201")
                || response.contains("R600")
                || response.contains("R601")
                || response.contains("R602")
                || response.contains("R610")
            ) {
                val amount = getTagValue(response, "04").toInt()
                val cardNumber = getTagValue(response, "02")
                val cardType = getTagValue(response, "54")
                val rrn = getTagValue(response, "37")

                // Postback
                onSaleApproved?.invoke(SaleResponse(amount, cardNumber, cardType, rrn))

            } else if (response.contains("R640")) {
                // MPQR
                val amount = getTagValue(response, "04").toInt()
                val cardNumber = ""
                val cardType = getTagValue(response, "54")

                // Postback
                onSaleApproved?.invoke(SaleResponse(amount, cardNumber, cardType, rrn = ""))
            } else {
                log("Command is not parsed: $response")
            }
        } else if (responseCode == "CT" || responseCode == "TA" || responseCode == "TN") {
            //isFinished = true
            onSaleCancel?.invoke()
        } else {
            onSaleError?.invoke(responseCode)
        }
    }

    private fun write(data: Byte) {
        val byteArray = mutableListOf<Byte>()
        byteArray.add(data)
        port.write(byteArray.toByteArray())
    }

    private fun write(data: ByteArray) {
        port.write(data)
    }

    private fun write(stringData: String) {
        val hexString = ByteUtil.str2HexString(stringData)
        val data = ByteUtil.hexStr2bytes(hexString)
        port.write(data)
    }

    private fun buildSaleCommand(amount: Int): ByteArray {
        val tags = mutableListOf<CommandTag>()
        tags.add(CommandTag(1, 3, CMD_TIME_OUT.toString()))
        tags.add(CommandTag(4, 12, amount.toString()))
        tags.add(CommandTag(57, 6, increaseIdentity().toString()))
        val cmd = CommandInformation("C200", "R200", tags)
        val cmdString = cmd.toString()
        log("---> $cmdString")
        return buildMainCommand(cmdString)
    }

    private fun buildPreauthEcrpeCommand(amount: Int): ByteArray {
        val tags = mutableListOf<CommandTag>()
        //tags.add(CommandTag(1, 3, CMD_TIME_OUT.toString()))
        tags.add(CommandTag(4, 12, amount.toString()))
        tags.add(CommandTag(57, 6, increaseIdentity().toString()))
        val cmd = CommandInformation("C201", "R201", tags)
        val cmdString = cmd.toString()
        log("---> $cmdString")
        return buildMainCommand(cmdString)
    }

    private fun buildPreauthCancelEcrpeCommand(amount: Int, rrn: String): ByteArray {
        val tags = mutableListOf<CommandTag>()
        //tags.add(CommandTag(1, 3, CMD_TIME_OUT.toString()))
        tags.add(CommandTag(4, 12, amount.toString()))
        tags.add(CommandTag(37, 12, rrn))
        tags.add(CommandTag(57, 6, increaseIdentity().toString()))
        val cmd = CommandInformation("C604", "R604", tags)
        val cmdString = cmd.toString()
        log("---> $cmdString")
        return buildMainCommand(cmdString)
    }

    private fun buildPreauthCaptureEcrpeCommand(amount: Int, rrn: String): ByteArray {
        val tags = mutableListOf<CommandTag>()
        //tags.add(CommandTag(1, 3, CMD_TIME_OUT.toString()))
        tags.add(CommandTag(4, 12, amount.toString()))
        tags.add(CommandTag(37, 12, rrn))
        tags.add(CommandTag(57, 6, increaseIdentity().toString()))
        val cmd = CommandInformation("C602", "R602", tags)
        val cmdString = cmd.toString()
        log("---> $cmdString")
        return buildMainCommand(cmdString)
    }

    private fun buildSaleEcrpeCommand(amount: Int, rrn: String): ByteArray {
        val tags = mutableListOf<CommandTag>()
        //tags.add(CommandTag(1, 3, CMD_TIME_OUT.toString()))
        tags.add(CommandTag(4, 12, amount.toString()))
        tags.add(CommandTag(37, 12, rrn))
        tags.add(CommandTag(57, 6, increaseIdentity().toString()))
        val cmd = CommandInformation("C600", "R600", tags)
        val cmdString = cmd.toString()
        log("---> $cmdString")
        return buildMainCommand(cmdString)
    }

    private fun buildMpqrCommand(amount: Int, walletLabel: String): ByteArray {
        val tags = mutableListOf<CommandTag>()
        tags.add(CommandTag(1, 3, CMD_TIME_OUT.toString()))
        tags.add(CommandTag(4, 12, amount.toString()))
        tags.add(CommandTag(54, -1, walletLabel))
        tags.add(CommandTag(57, 6, increaseIdentity().toString()))
        val cmd = CommandInformation("C640", "R640", tags)
        val cmdString = cmd.toString()
        log("---> $cmdString")
        val cmdByte = buildMainCommand(cmdString)
        return cmdByte
    }

    private fun builCpasCommand(amount: Int): ByteArray {
        val tags = mutableListOf<CommandTag>()
        tags.add(CommandTag(1, 3, CMD_TIME_OUT.toString()))
        tags.add(CommandTag(4, 12, amount.toString()))
        tags.add(CommandTag(57, 6, increaseIdentity().toString()))
        val cmd = CommandInformation("C610", "R610", tags)
        val cmdString = cmd.toString()
        log("---> $cmdString")
        val cmdByte = buildMainCommand(cmdString)
        return cmdByte
    }

    private fun buildSettlementCommand(): ByteArray {
        val tags = mutableListOf<CommandTag>()
        tags.add(CommandTag(57, 6, increaseIdentity().toString()))
        val cmd = CommandInformation("C700", "R700", tags)
        val cmdString = cmd.toString()
        log("---> $cmdString")
        val cmdByte = buildMainCommand(cmdString)
        return cmdByte
    }

    private fun buildMainCommand(mainCommand: String): ByteArray {
        val cmd = mutableListOf<Byte>()
        val hexString = ByteUtil.str2HexString(mainCommand)
        val mainCmdByte = ByteUtil.hexStr2bytes(hexString)
        cmd.addAll(mainCmdByte.toList())
        // End byte
        cmd.add(0x03)
        // Checksum
        val crc = xorChecksum(cmd.toByteArray())
        cmd.add(crc)
        // First byte
        cmd.add(0, 0x02)

        return cmd.toByteArray()
    }

    private fun increaseIdentity(): Int {
        CmdIdentity++
        if (CmdIdentity === 999999) CmdIdentity = 0
        return CmdIdentity
    }

    private fun xorChecksum(byteArray: ByteArray): Byte {
        var returnData: Byte = 0x00

        for (byte in byteArray) {
            returnData = returnData xor byte
        }

        return returnData
    }

    fun getTagValue(responseCommand: String, inlineTag: String): String {
        var returnValue = ""
        try {
            val command = responseCommand.replace("\u0002", "")
            var lastLength = 4
            for (i in 4..command.length) {
                if (i < lastLength) {
                    continue
                }
                val tag = command.substring(i, i + 2)
                val length = command.substring(i + 2, i + 4).toInt()
                lastLength = i + 4 + length
                val value = command.substring(i + 4, lastLength)
                if (tag == inlineTag) {
                    returnValue = value
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            log(e.toString())
        }
        return returnValue
    }

    fun getTagListValue(command: String): HashMap<String, String> {
        val hashMap = HashMap<String, String>()

        try {
            var lastLength = 4
            for (i in 4..command.length) {
                if (i < lastLength) {
                    continue
                }
                val tag = command.substring(i, i + 2)
                val length = command.substring(i + 2, i + 4).toInt()
                lastLength = i + 4 + length
                val value = command.substring(i + 4, lastLength)

                hashMap.put(tag, value)
            }
        } catch (ex: StringIndexOutOfBoundsException) {

        }

        return hashMap
    }

    private fun log(s: String) {
        if (logger == null) {
            Log.d("IM30Interface", s)
        } else {
            logger?.invoke(s)
        }
    }

    class CommandInformation(
        var command: String,
        var responseCommand: String,
        var tags: List<CommandTag>
    ) {
        override fun toString(): String {
            var tagString = ""

            for (tag in tags) {
                tagString += tag.toString()
            }
            return command + tagString
        }
    }

    class CommandTag(var tag: Int, var length: Int, var value: String) {
        override fun toString(): String {
            if (length == -1) {
                val t = this.tag.toString().padStart(2, '0')
                val l = this.value.length.toString().padStart(2, '0')
                val v = this.value

                return "${t}${l}${v}"
            } else {
                val t = this.tag.toString().padStart(2, '0')
                val l = this.length.toString().padStart(2, '0')
                val v = this.value.toString().padStart(length, '0')
                return "${t}${l}${v}"
            }
        }
    }

    data class SaleResponse(
        var amount: Int,
        var cardNumber: String,
        var cardType: String,
        var rrn: String
    )

    enum class MpqrWalletLabel(val value: String) {
        ACTIVESG("ACTIVESG"),
        DASH("DASH"),
        ALIPAY("ALIPAY"),
        GRABPAY("GRABPAY"),
        WECHAT("WECHAT"),
        UNIONPAYQR("UNIONPAYQR"),
        MOMOPAY("MOMOPAY"),
        DBSMAX_PAYNOW("DBSMAX PAYNOW"),
        RAZERPAY("RAZERPAY"),
        LIQUIDPAY("LIQUIDPAY"),
        OCBC_PAYNOW("OCBC PAYNOW"),

        // DEMO
        DBSMAXDEMO("DBSMAXDEMO"),
        DASHDEMO("DASHDEMO"),
        ALIPAYDEMO("ALIPAYDEMO"),
        GRABPAYDEMO("GRABPAYDEMO"),
        WECHATDEMO("WECHATDEMO")
    }
}