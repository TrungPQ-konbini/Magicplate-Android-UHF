package com.konbini.magicplateuhf.data.remote.product.response

import com.google.gson.annotations.SerializedName

data class Option(
    @SerializedName("_name")
    val name: String = "",
    @SerializedName("_type")
    val type: String = "",
    @SerializedName("_required")
    val required: String = "",
    @SerializedName("_min_op")
    val minOp: String = "",
    @SerializedName("_max_op")
    val maxOp: String = "",
    @SerializedName("_options")
    val options: ArrayList<OptionItem>? = null
)
