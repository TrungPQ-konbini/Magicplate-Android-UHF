package com.konbini.magicplateuhf.data.remote.category.response

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("slug")
    val slug: String?,
    @SerializedName("parent")
    val parent: Int?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("display")
    val display: String?,
    @SerializedName("menu_order")
    val menuOrder: Int?,
    @SerializedName("count")
    val count: Int?,
)
