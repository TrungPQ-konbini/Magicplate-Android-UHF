package com.konbini.magicplateuhf.hardware

import android.os.SystemClock
import android.util.Log
import com.konbini.magicplateuhf.utils.ByteUtil
import com.nativec.tools.SerialPort
import com.nativec.tools.SerialPortFinder
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class SerialPortInterface {
    lateinit var inputStream: InputStream
    lateinit var outputStream: OutputStream
    var dataReceiveHandler: ISerialPortDataReceiveHandler? = null
    var logger: ISerialPortLogger? = null

    companion object {
        lateinit var instance: SerialPortInterface
        lateinit var port: SerialPort
        lateinit var portFinder: SerialPortFinder
        lateinit var t: Thread
        const val TAG = "SerialPortInterface"

    }

    init {
        instance = this
        portFinder = SerialPortFinder()
    }

    fun open(portName: String, baurate: Int): Boolean {
        return try {
            log("Opening port $portName | Baurate $baurate")
            var file = File(portName)
            port = SerialPort(file, baurate,0)
            if (port != null) {
                log("Open Port Success")
                outputStream = port.outputStream
                inputStream = port.inputStream
                true
            } else {
                log("Open Port Failed")
                false
            }
        } catch (ex: Exception) {
            false
        }
    }

    fun getPorts(): Array<String> {
        return portFinder.allDevicesPath
    }

    var isThreadRunning: Boolean = false
    fun startReadData() {
        try {
            if (port != null) {
                isThreadRunning = true
                val received = ByteArray(1024)
                var size: Int
                t = Thread {
                    log("Start reading data thread")
                    while (true) {
                        try {
                            if (!isThreadRunning) {
                                log("Thread stopped")
                                break
                            }
                            val available: Int = inputStream!!.available()
                            if (available > 0) {
                                size = inputStream!!.read(received)
                                if (size > 0) {
                                    //onDataReceive(received, size)
                                    dataReceiveHandler!!.onDataReceive(received, size)
                                }
                            } else {
                                SystemClock.sleep(1)
                            }
                        } catch (e: IOException) {
                            Log.e("ERROR", e.toString())
                            Log.d(TAG, "Failed to read data. $e")
                        }
                    }
                }

                t.start()
                log("Thread data started!")
            } else {
                log("Failed to start read data. Port is null")
            }
        } catch (ex: Exception) {
            log("Failed to start read data. $ex")
            ex.printStackTrace()
        }
    }

    fun stopReadData() {
        log("Stop Read Data")
        isThreadRunning = false
    }

    fun write(data: ByteArray) {
        val hexStr: String = ByteUtil.bytes2HexStr(data, 0, data.count())
        log("---> $hexStr")
        outputStream!!.write(data)
    }

    fun read(): ByteArray {
        val received = ByteArray(1024)
        var size: Int
        val available: Int = inputStream!!.available()
        if (available > 0) {
            size = inputStream!!.read(received)
            if (size > 0) {
                var data: ByteArray = received.copyOf(size)
                val hexStr: String = ByteUtil.bytes2HexStr(received, 0, size)
                log("<--- $hexStr")
                return data
            }
        }
        Log.d(TAG, "<--- [NO DATA]")

        return ByteArray(0)
    }

    private fun log(data: String) {
        if(logger == null){
            Log.d(TAG, data)
        }else {
            logger!!.log(data)
        }

    }

    private fun log(exception: java.lang.Exception) {
        if(logger == null){
            Log.e(TAG, exception.toString())
        }else {
            logger!!.log(exception)
        }

    }
}

interface ISerialPortLogger {
    fun log(data: String)
    fun log(exception: java.lang.Exception)
}

interface ISerialPortDataReceiveHandler {
    fun onDataReceive(received: ByteArray, size: Int)
}