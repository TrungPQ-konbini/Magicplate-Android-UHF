package com.konbini.magicplateuhf.data.enum

enum class PaymentState {
    /**
     * Payment initial
     * Init transaction
     **/
    Init,
    /**
     * Preparing for the new transaction(get product to sale)
     * Init transaction
     **/
    Preparing,
    /**
     * Payment ready
     * when transaction is valid to start payment.
     **/
    ReadyToPay,
    /**
     * When card scanned, cash/coin start to inserting => InProgress
     **/
    InProgress,
    /**
     * Payment success
     **/
    Success,
    /**
     * Payment cancel
     **/
    Cancelled,
    /**
     * Payment Rejected
     **/
    Rejected,
    /**
     * Payment Error
     **/
    Error
}