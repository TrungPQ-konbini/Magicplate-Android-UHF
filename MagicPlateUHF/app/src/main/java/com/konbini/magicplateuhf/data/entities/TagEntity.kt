package com.konbini.magicplateuhf.data.entities

data class TagEntity(
    var strEPC: String? = null,
    var plateModel: String? = null,
    var plateModelTitle: String? = null,
    var serialNumber: String? = null,
    var paidDate: String? = null,
    var timestamp: String? = null,
    var customPrice: String = "",
    var lastUpdate: Long = 0,
    var isWriteFalse: Boolean = false
)
