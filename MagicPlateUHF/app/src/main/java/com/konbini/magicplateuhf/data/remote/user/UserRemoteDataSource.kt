package com.konbini.magicplateuhf.data.remote.user

import android.util.Log
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import com.konbini.magicplateuhf.data.remote.user.request.GetAllUserRequest
import javax.inject.Inject

class UserRemoteDataSource @Inject constructor(
    private val userService: UserService
) : BaseDataSource() {
    suspend fun getAllUsers(url: String, bodyRequest: GetAllUserRequest) = getResult {
        val path = "$url${AppSettings.APIs.GetAllUser}"
        Log.e("getAllUsers", path)
        userService.getAllUsers(path, bodyRequest)
    }
}