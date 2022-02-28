package com.konbini.magicplateuhf.utils

import android.content.Context
import com.konbini.magicplateuhf.R

class ErrorCodeIM30 {
    companion object {
        fun handleMessageIuc(code: String, context: Context): String {
            return when (code) {
                "EC" -> {
                    String.format(context.getString(R.string.message_error_payment_failed), "CARD IS EXPIRED")
                }
                "CE" -> {
                    String.format(context.getString(R.string.message_error_payment_failed), "CONNECTION ERROR")
                }
                "RE" -> {
                    String.format(context.getString(R.string.message_error_payment_failed), "RECORD NOT FOUND")
                }
                "HE" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "WRONG HOST NUMBER PROVIDED"
                    )
                }
                "LE" -> {
                    String.format(context.getString(R.string.message_error_payment_failed), "LINE ERROR")
                }
                "VB" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "TRANSACTION ALREADY VOIDED"
                    )
                }
                "FE" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "FILE EMPTY / NO TRANSACTION TO VOID"
                    )
                }
                "WC" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "CARD NUMBER DOES NOT MATCH"
                    )
                }
                "TA" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "TRANSACTION ABORTED BY USER"
                    )
                }
                "AE" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "AMOUNT DID NOT MATCH"
                    )
                }
                "XX" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "TERMINAL NOT PROPERLY SETUP "
                    )
                }
                "DL" -> {
                    String.format(context.getString(R.string.message_error_payment_failed), "LOGON NOT DONE")
                }
                "BT" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "BAD TLV COMMAND FORMAT"
                    )
                }
                "IS" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "TRANSACTION NOT FOUND, INQUIRY SUCCESSFUL"
                    )
                }
                "CD" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "CARD DECLINED TRANSACTION"
                    )
                }
                "LH" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "LOYALTY HOST IS TEMPORARY OFFLINE"
                    )
                }
                "IC",
                "IN" -> {
                    String.format(context.getString(R.string.message_error_payment_failed), "INVALID CARD")
                }
                "CO" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "CARD NOT READ PROPERLY"
                    )
                }
                "TL" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "TOP UP LIMIT EXCEEDED"
                    )
                }
                "PL" -> {
                    String.format(
                        context.getString(R.string.message_error_payment_failed),
                        "PAYMENT LIMIT EXCEEDED"
                    )
                }
                else -> {
                    String.format(context.getString(R.string.message_error_unknown_error), code)
                }
            }
        }
    }
}