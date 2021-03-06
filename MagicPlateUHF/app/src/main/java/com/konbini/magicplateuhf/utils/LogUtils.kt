package com.konbini.magicplateuhf.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object LogUtils {
    private const val TAG = "LOGGER"

    fun d(message: String) {
        val ste = Throwable().stackTrace
        val text = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()]"
        Log.d(TAG, text + message)
    }

    fun i(message: String) {
        val ste = Throwable().stackTrace
        val text = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()]"
        Log.i(TAG, text + message)
    }

    fun e(message: String) {
        val ste = Throwable().stackTrace
        val text =
            "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] !!!WARNING "
        Log.e(TAG, text + message)
    }

    fun logInfo(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message, MainApplication.shared().applicationContext)
    }

    fun logError(e: Exception) {
        val ste = Throwable().stackTrace
        val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()

        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + stacktrace
        e.printStackTrace()
        writeLog(message, MainApplication.shared().applicationContext)
    }

    fun logMagicPlate(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message, MainApplication.shared().applicationContext, "MagicPlate")
    }

    fun logCrash(r: Throwable) {
        val ste = r.stackTrace
        val stacktrace = StringWriter().also { r.printStackTrace(PrintWriter(it)) }.toString().trim()

        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + stacktrace
        Log.e("Crash", message)

        writeLog(message, MainApplication.shared().applicationContext, "Crash")
    }

    fun logOffline(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message, MainApplication.shared().applicationContext, "OfflineData")
    }

    fun logReader(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        if (AppSettings.Options.AllowGetReaderLog)
            writeLog(message, MainApplication.shared().applicationContext, "Reader")
    }

    fun logApi(log: String) {
        writeLog(log, MainApplication.shared().applicationContext, "Api")
    }

    fun logLogcat(context: Context) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendarTime = Calendar.getInstance().time
        val currentDate = formatter.format(calendarTime)

        val filename = "${currentDate}_Logcat.txt"
        val outputFile = File(context.externalCacheDir, filename)
        Runtime.getRuntime().exec("logcat -f ${outputFile.absolutePath} *:W")

    }

    @SuppressLint("SimpleDateFormat")
    private fun writeLog(text: String?, context: Context, fileName: String = "Info") {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        val formatterTime = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        val calendarTime = Calendar.getInstance().time
        val currentDate = formatter.format(calendarTime)

        val dirMain = File(context.getExternalFilesDir(""), "Logs")
        var success = true
        if (!dirMain.exists())
            try {
                success = dirMain.mkdir()
            } catch (ex: IOException) {
                Log.d("ERROR", ex.message.toString())
            }

        if (success) {
            val fileLog = File(dirMain, "${currentDate}_${fileName}.txt")
            try {
                if (!fileLog.exists())
                    success = fileLog.createNewFile()

                if (success) {
                    val ste = Throwable().stackTrace
                    val buf = BufferedWriter(FileWriter(fileLog, true))
                    val msg = "${formatterTime.format(calendarTime)} : $text"
                    buf.append(msg)
                    buf.newLine()
                    buf.close()
                    Log.d("Logger-$fileName", msg)
                }
            } catch (ex: IOException) {
                Log.d("ERROR", ex.message.toString())
            }
        } else {
            Log.d("ERROR", "Failed to create directory")
        }
    }
}