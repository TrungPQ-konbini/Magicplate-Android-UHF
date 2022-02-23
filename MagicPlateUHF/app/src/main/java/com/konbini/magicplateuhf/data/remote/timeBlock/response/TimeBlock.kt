package com.konbini.magicplateuhf.data.remote.timeBlock.response

import com.google.gson.annotations.SerializedName

data class TimeBlock(
    @SerializedName("time_block_id")
    val timeBlockId: String?,
    @SerializedName("from_hour")
    var fromHour: String?,
    @SerializedName("to_hour")
    var toHour: String?,
    @SerializedName("time_block_title")
    val timeBlockTitle: String?
)
