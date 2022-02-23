package com.konbini.magicplateuhf.data.remote.plateModel.response

import com.google.gson.annotations.SerializedName

data class PlateModelResponse(
    @SerializedName("results")
    val results: List<PlateModel> = listOf()
)
