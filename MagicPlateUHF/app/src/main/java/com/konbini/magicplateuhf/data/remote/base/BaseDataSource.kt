package com.konbini.magicplateuhf.data.remote.base

import com.konbini.magicplateuhf.utils.Resource
import retrofit2.Response

abstract class BaseDataSource {
    protected suspend fun <T> getResult(call: suspend () -> Response<T>): Resource<T> {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) return Resource.success(body)
            }
            return error(" ${response.code()} ${response.message()}")
        } catch (e: Exception) {
            return error(e.message ?: e.toString())
        }
    }

    protected suspend fun <T> getResultWithError(call: suspend () -> Response<T>): Resource<T> {
        try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) return Resource.success(body)
            }

            return if (response.errorBody() != null) {
                val bodyError = response.errorBody()
                error(bodyError!!.string(), true)
            } else {
                error("${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            return error(e.message ?: e.toString())
        }
    }

    private fun <T> error(message: String, isJson: Boolean = false): Resource<T> {
        if (isJson) {
            return Resource.error(message)
        }
        return Resource.error("Network call has failed for a following reason: $message")
    }
}