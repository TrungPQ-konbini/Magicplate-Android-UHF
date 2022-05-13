package com.konbini.magicplateuhf.data.remote.user

import com.konbini.magicplateuhf.data.remote.user.request.GetAllUserRequest
import com.konbini.magicplateuhf.data.remote.user.response.GetAllUserResponse
import retrofit2.Response
import retrofit2.http.*

interface UserService {
    @POST
    suspend fun getAllUsers(
        @Url url: String,
        @Body bodyRequest: GetAllUserRequest
    ): Response<GetAllUserResponse>
}