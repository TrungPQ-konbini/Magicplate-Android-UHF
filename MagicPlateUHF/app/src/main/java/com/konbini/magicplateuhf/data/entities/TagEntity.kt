package com.konbini.magicplateuhf.data.entities

data class TagEntity(
    var strEPC: String? = null,
    var plateModel: String? = null,
    var plateModelTitle: String? = null,
    var serialNumber: String? = null,
    var paidDate: String? = null,
    var paidSession: String? = null,
    var customPrice: String = "",
    var lastUpdate: Long = 0
) {

}
