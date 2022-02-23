package com.konbini.magicplateuhf.data.remote.timeBlock.response

import com.google.gson.annotations.SerializedName

data class TimeBlockResponse(
    @SerializedName("results")
    val results: List<TimeBlock> = listOf()
)