package com.konbini.magicplateuhf.data.remote.user.response

import com.google.gson.annotations.SerializedName

data class GetAllUserResponse(
    @SerializedName("result")
    var result: String,
    @SerializedName("success")
    var success: Boolean,
    @SerializedName("message")
    var message: String,
    @SerializedName("data")
    var data: List<UserResponse>
)

data class UserResponse(
    @SerializedName("id")
    var id: String,
    @SerializedName("display_name")
    var displayName: String,
    @SerializedName("roles")
    var roles: List<String>,
    @SerializedName("ccw_id1")
    var ccwId1: String,
    @SerializedName("ccw_id2")
    var ccwId2: String,
    @SerializedName("ccw_id3")
    var ccwId3: String
)