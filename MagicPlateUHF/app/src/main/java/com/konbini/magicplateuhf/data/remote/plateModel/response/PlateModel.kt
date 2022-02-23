package com.konbini.magicplateuhf.data.remote.plateModel.response

import com.google.gson.annotations.SerializedName

data class PlateModel(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("plate_model_id")
    val plateModelId: String = "",
    @SerializedName("plate_model_code")
    val plateModelCode: String = "",
    @SerializedName("plate_model_title")
    val plateModelTitle: String = ""
)
