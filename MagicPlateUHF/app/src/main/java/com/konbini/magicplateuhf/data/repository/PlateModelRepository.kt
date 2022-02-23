package com.konbini.magicplateuhf.data.repository

import com.konbini.magicplateuhf.data.entities.PlateModelEntity
import com.konbini.magicplateuhf.data.local.plateModel.PlateModelDao
import com.konbini.magicplateuhf.data.remote.plateModel.PlateModelRemoteDataSource
import javax.inject.Inject

class PlateModelRepository @Inject constructor(
    private val localPlateModelDataSource: PlateModelDao,
    private val remoteDataSource: PlateModelRemoteDataSource
) {
    suspend fun syncPlateModels(url: String) =
        remoteDataSource.syncPlateModels(url)

    suspend fun getAll() = localPlateModelDataSource.getAll()

    suspend fun insert(plateModelEntity: PlateModelEntity) =
        localPlateModelDataSource.insert(plateModelEntity)

    suspend fun deleteAll() = localPlateModelDataSource.deleteAll()
}