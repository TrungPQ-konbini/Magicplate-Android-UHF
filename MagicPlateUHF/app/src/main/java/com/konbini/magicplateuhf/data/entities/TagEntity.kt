package com.konbini.magicplateuhf.data.entities

data class TagEntity(
    var strEPC: String? = null,
    var uuid: String? = null,
    var modelNumber: String? = null,
    var modelName: String? = null,
    var customPrice: String = ""
)
