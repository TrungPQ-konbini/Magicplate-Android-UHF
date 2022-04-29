package com.konbini.magicplateuhf.data.remote.plateModel.response

import com.google.gson.annotations.SerializedName

data class SetPlateModelResponse(
    @SerializedName("result")
    var result: String? = null,
    @SerializedName("message")
    var message: String? = null,
    @SerializedName("data")
    var data: ArrayList<Data> = arrayListOf()
)

data class Data(
    @SerializedName("plate_model_id")
    var plateModelId: Int? = null,
    @SerializedName("last_plate_serial")
    var lastPlateSerial: String? = null
)