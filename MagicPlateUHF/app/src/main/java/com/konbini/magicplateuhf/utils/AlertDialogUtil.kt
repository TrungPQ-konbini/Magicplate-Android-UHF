package com.konbini.magicplateuhf.utils

import android.content.Context
import com.developer.kalert.KAlertDialog
import com.konbini.magicplateuhf.R

object AlertDialogUtil {
    fun showSuccess(msg: String, context: Context) {
        val pDialog = KAlertDialog(context, KAlertDialog.SUCCESS_TYPE)
        pDialog.progressHelper.barColor = context.getColor(R.color.american_green)
        pDialog.titleText = "Success"
        pDialog.contentText = msg
        pDialog.setCancelable(false)
        pDialog.setCanceledOnTouchOutside(true)
        pDialog.show()
    }

    fun showError(msg: String, context: Context, title: String = "Error") {
        val pDialog = KAlertDialog(context, KAlertDialog.ERROR_TYPE)
        pDialog.progressHelper.barColor = context.getColor(R.color.red)
        pDialog.titleText = title
        pDialog.contentText = msg
        pDialog.setCancelable(false)
        pDialog.setCanceledOnTouchOutside(true)
        pDialog.show()
    }

    fun showWarning(msg: String, context: Context, title: String = "Warning") {
        val pDialog = KAlertDialog(context, KAlertDialog.WARNING_TYPE)
        pDialog.progressHelper.barColor = context.getColor(R.color.yellow)
        pDialog.titleText = title
        pDialog.contentText = msg
        pDialog.setCancelable(true)
        pDialog.setCanceledOnTouchOutside(true)
        pDialog.show()
    }

    fun showProgress(msg: String, context: Context) {
        val pDialog = KAlertDialog(context, KAlertDialog.PROGRESS_TYPE)
        pDialog.progressHelper.barColor = context.getColor(R.color.grey)
        pDialog.titleText = "Info"
        pDialog.contentText = msg
        pDialog.setCancelable(false)
        pDialog.setCanceledOnTouchOutside(true)
        pDialog.show()
    }
}