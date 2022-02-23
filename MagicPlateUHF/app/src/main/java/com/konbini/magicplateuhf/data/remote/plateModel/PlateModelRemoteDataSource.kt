package com.konbini.magicplateuhf.data.remote.plateModel

import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import javax.inject.Inject

class PlateModelRemoteDataSource@Inject constructor(
    private val plateModelService: PlateModelService
): BaseDataSource() {
    suspend fun syncPlateModels(
        url: String
    ) = getResult {
        val api = AppSettings.APIs.ListAllPlateModel
        val path = "$url$api"
        plateModelService.syncPlateModels(path)
    }
}