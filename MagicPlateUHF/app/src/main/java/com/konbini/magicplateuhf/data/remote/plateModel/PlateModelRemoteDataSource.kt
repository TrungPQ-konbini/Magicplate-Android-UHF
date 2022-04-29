package com.konbini.magicplateuhf.data.remote.plateModel

import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import com.konbini.magicplateuhf.data.remote.plateModel.request.SetPlateModelRequest
import javax.inject.Inject

class PlateModelRemoteDataSource@Inject constructor(
    private val plateModelService: PlateModelService
): BaseDataSource() {
    suspend fun syncPlateModels(
        url: String
    ) = getResult {
        val api = AppSettings.APIs.GetPlateModelData
        val path = "$url$api"
        plateModelService.syncPlateModels(path)
    }

    suspend fun setPlateModels(
        url: String,
        bodyRequest: SetPlateModelRequest
    ) = getResult {
        val api = AppSettings.APIs.SetPlateModelData
        val path = "$url$api"
        plateModelService.setPlateModels(path, bodyRequest)
    }
}