package com.konbini.magicplateuhf.data.remote.plateModel.request

import com.google.gson.annotations.SerializedName

data class SetPlateModelRequest(
    @SerializedName("access_token")
    var accessToken: String? = null,
    @SerializedName("data")
    var data: ArrayList<Data> = arrayListOf()
)

data class Data(
    @SerializedName("plate_model_id")
    var plateModelId: Int? = null,
    @SerializedName("last_plate_serial")
    var lastPlateSerial: String? = null
)