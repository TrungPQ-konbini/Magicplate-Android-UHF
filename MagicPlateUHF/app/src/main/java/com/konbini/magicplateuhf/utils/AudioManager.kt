package com.konbini.magicplateuhf.utils

import android.content.Context
import android.media.MediaPlayer
import com.konbini.magicplateuhf.R

class AudioManager(private val context: Context) {

    companion object {
        lateinit var instance: AudioManager
    }
    init {
        instance = this
    }

    // Instance variables
    private var mpPayment: MediaPlayer? = null
    private var mpError: MediaPlayer? = null
    private var mpBuzzer: MediaPlayer? = null
    private var mpSuccess: MediaPlayer? = null
    private var mpDoNotChangeItem: MediaPlayer? = null
    private var mpDoNotRemoveYourTray: MediaPlayer? = null
    private var mpIncorrectDetectedCard: MediaPlayer? = null
    private var mpPaymentFailedTryAgain: MediaPlayer? = null
    private var mpPaymentSuccess: MediaPlayer? = null
    private var mpPaymentFailed: MediaPlayer? = null
    private var mpPlaceTrayInTheBox: MediaPlayer? = null
    private var mpScanQR: MediaPlayer? =null
    private var mpTransactionTimeout: MediaPlayer? = null
    private var mpProcessingPayment: MediaPlayer? =null
    private var mpProcessingPaymentDoNotChangeItem: MediaPlayer? =null
    private var mpPleaseScanYourCard: MediaPlayer? = null
    private var mpPleaseTapCard: MediaPlayer? = null
    private var mpPleaseTapCardAgain: MediaPlayer? = null
    private var mpCartIsEmpty: MediaPlayer? = null
    private var mpPleaseLookIntoCamera: MediaPlayer? = null

    fun soundCartIsEmpty() {
        if (mpCartIsEmpty == null) {
            mpCartIsEmpty = MediaPlayer.create(context, R.raw.cart_is_empty);
        }
        mpCartIsEmpty?.start()
    }

    fun soundPayment() {
        if (mpPayment == null) {
            mpPayment = MediaPlayer.create(context, R.raw.payment);
        }
        mpPayment?.start()
    }

    fun soundBuzzer() {
        if (mpBuzzer == null) {
            mpBuzzer = MediaPlayer.create(context, R.raw.buzzer);
        }
        mpBuzzer?.start()
    }

    fun soundError() {
        if (mpError == null) {
            mpError = MediaPlayer.create(context, R.raw.error);
        }
        mpError?.start()
    }

    fun soundSuccess() {
        if (mpSuccess == null) {
            mpSuccess = MediaPlayer.create(context, R.raw.success);
        }
        mpSuccess?.start()
    }

    fun soundDoNotRemoveYourTray() {
        if (mpDoNotRemoveYourTray == null) {
            mpDoNotRemoveYourTray = MediaPlayer.create(context, R.raw.do_not_remove_your_tray);
        }
        mpDoNotRemoveYourTray?.start()
    }

    fun soundDoNotChangeItem() {
        if (mpDoNotChangeItem == null) {
            mpDoNotChangeItem = MediaPlayer.create(context, R.raw.do_not_change_item);
        }
        mpDoNotChangeItem?.start()
    }

    fun soundIncorrectDetectedCard() {
        if (mpIncorrectDetectedCard == null) {
            mpIncorrectDetectedCard = MediaPlayer.create(context, R.raw.incorrect_card_detected_please_check_the_card_type);
        }
        mpIncorrectDetectedCard?.start()
    }

    fun soundPaymentFailedTryAgain() {
        if (mpPaymentFailedTryAgain == null) {
            mpPaymentFailedTryAgain = MediaPlayer.create(context, R.raw.payment_failed_please_try_again_or_contact_staff);
        }
        mpPaymentFailedTryAgain?.start()
    }

    fun soundPaymentSuccess() {
        if (mpPaymentSuccess == null) {
            mpPaymentSuccess = MediaPlayer.create(context, R.raw.payment_successful);
        }
        mpPaymentSuccess?.start()
    }

    fun soundPaymentFailed() {
        if (mpPaymentFailed == null) {
            mpPaymentFailed = MediaPlayer.create(context, R.raw.payment_failed);
        }
        mpPaymentFailed?.start()
    }

    fun soundPlaceTrayInTheBox() {
        if (mpPlaceTrayInTheBox == null) {
            mpPlaceTrayInTheBox = MediaPlayer.create(context, R.raw.please_place_tray_in_the_box);
        }
        mpPlaceTrayInTheBox?.start()
    }

    fun soundScanQR() {
        if (mpScanQR == null) {
            mpScanQR = MediaPlayer.create(context, R.raw.please_scan_the_qr);
        }
        mpScanQR?.start()
    }

    fun soundTransactionTimeout() {
        if (mpTransactionTimeout == null) {
            mpTransactionTimeout = MediaPlayer.create(context, R.raw.transaction_timed_out_please_try_again);
        }
        mpTransactionTimeout?.start()
    }

    fun soundProcessingPayment() {
        if (mpProcessingPayment == null) {
            mpProcessingPayment = MediaPlayer.create(context, R.raw.processing_payment);
        }
        mpProcessingPayment?.start()
    }

    fun soundProcessingPaymentDoNotChangeItem() {
        if (mpProcessingPaymentDoNotChangeItem == null) {
            mpProcessingPaymentDoNotChangeItem = MediaPlayer.create(context, R.raw.processing_payment_do_not_change_item);
        }
        mpProcessingPaymentDoNotChangeItem?.start()
    }

    fun soundPleaseScanYourCard() {
        if (mpPleaseScanYourCard == null) {
            mpPleaseScanYourCard = MediaPlayer.create(context, R.raw.please_scan_your_card);
        }
        mpPleaseScanYourCard?.start()
    }

    fun soundPleaseTapCard() {
        if (mpPleaseTapCard == null) {
            mpPleaseTapCard = MediaPlayer.create(context, R.raw.please_tap_card);
        }
        mpPleaseTapCard?.start()
    }

    fun soundPleaseTapCardAgain() {
        if (mpPleaseTapCardAgain == null) {
            mpPleaseTapCardAgain = MediaPlayer.create(context, R.raw.please_tap_card_again)
        }
        mpPleaseTapCardAgain?.start()
    }

    fun soundPleaseLookIntoCamera() {
        if (mpPleaseLookIntoCamera == null) {
            mpPleaseLookIntoCamera = MediaPlayer.create(context, R.raw.please_look_into_camera);
        }
        mpPleaseLookIntoCamera?.start()
    }
}