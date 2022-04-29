package com.konbini.magicplateuhf.data.remote.plateModel

import com.konbini.magicplateuhf.data.remote.plateModel.request.SetPlateModelRequest
import com.konbini.magicplateuhf.data.remote.plateModel.response.PlateModelResponse
import com.konbini.magicplateuhf.data.remote.plateModel.response.SetPlateModelResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface PlateModelService {
    @GET
    suspend fun syncPlateModels(
        @Url url: String
    ): Response<PlateModelResponse>

    @POST
    suspend fun setPlateModels(
        @Url url: String,
        @Body bodyRequest: SetPlateModelRequest
    ): Response<SetPlateModelResponse>
}