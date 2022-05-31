package com.konbini.magicplateuhf.data.enum

enum class PaymentModeType(val value: String) {
    KONBINI_WALLET("KONBINI_WALLET"),
    MASTER_CARD("MASTER_CARD"),
    EZ_LINK("EZ_LINK"),
    PAY_NOW("PAY_NOW"),
    CASH("CASH"),
    DISCOUNT("DISCOUNT")
}