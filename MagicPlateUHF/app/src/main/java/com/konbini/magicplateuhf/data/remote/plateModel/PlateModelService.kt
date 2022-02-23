package com.konbini.magicplateuhf.data.remote.plateModel

import com.konbini.magicplateuhf.data.remote.plateModel.response.PlateModelResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface PlateModelService {
    @GET
    suspend fun syncPlateModels(
        @Url url: String
    ): Response<PlateModelResponse>
}