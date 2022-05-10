package com.konbini.magicplateuhf.utils

import android.content.Context
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R

class UhfUtil {
    companion object {
        fun setAccessEpcMatch(tag: String, context: Context, errorMessage: String): Int {
            var btAryEpc: ByteArray? = null
            btAryEpc = try {
                val result = StringTool.stringToStringArray(tag.uppercase(), 2)
                StringTool.stringArrayToByteArray(result, result.size)
            } catch (ex: Exception) {
                AlertDialogUtil.showError(errorMessage, context)
                LogUtils.logError(ex)
                return - 1
            }
            if (btAryEpc == null) {
                AlertDialogUtil.showError(
                    errorMessage,
                    context
                )
                return - 1
            }
            // TODO: ABC
//            return MainApplication.mReaderUHF.setAccessEpcMatch(
//                0x01,
//                (btAryEpc.size and 0xFF).toByte(), btAryEpc
//            )
            return 0
        }

        fun writeTag(tag: String, context: Context, errorMessage: String): Int {
            /*
             * 0x00: area password
             * 0x01: area epc
             * 0x02: area tid
             * 0x03: area user
             */
            val btMemBank: Byte = 0x01 // Fix access area EPC
            val btWordAdd: Byte = 0x02
            var btWordCnt: Byte = 0x00
            val btAryPassWord: ByteArray =
                byteArrayOf(0x00, 0x00, 0x00, 0x00) // Fix password is 00000000

            var btAryData: ByteArray? = null
            var result: Array<String>? = null
            try {
                result = StringTool.stringToStringArray(tag.uppercase(), 2)
                btAryData = StringTool.stringArrayToByteArray(result, result.size)
                btWordCnt = (result.size / 2 + result.size % 2 and 0xFF).toByte()
            } catch (ex: Exception) {
                AlertDialogUtil.showError(errorMessage, context)
                LogUtils.logError(ex)
                return -1
            }

            if (btAryData == null || btAryData.isEmpty()) {
                AlertDialogUtil.showError(errorMessage, context)
                return -1
            }
            // TODO: ABC
//            return MainApplication.mReaderUHF.writeTag(
//                0x01,
//                btAryPassWord,
//                btMemBank,
//                btWordAdd,
//                btWordCnt,
//                btAryData
//            )
            return 0
        }
    }
}