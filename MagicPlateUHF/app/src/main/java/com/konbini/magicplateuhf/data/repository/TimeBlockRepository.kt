package com.konbini.magicplateuhf.data.repository

import com.konbini.magicplateuhf.data.entities.TimeBlockEntity
import com.konbini.magicplateuhf.data.local.timeBlock.TimeBlockDao
import com.konbini.magicplateuhf.data.remote.timeBlock.TimeBlockRemoteDataSource
import javax.inject.Inject

class TimeBlockRepository @Inject constructor(
    private val localTimeBlockDataSource: TimeBlockDao,
    private val remoteDataSource: TimeBlockRemoteDataSource
) {
    suspend fun syncTimeBlocks(url: String) =
        remoteDataSource.syncTimeBlocks(url)

    suspend fun getAll() = localTimeBlockDataSource.getAll()

    suspend fun insert(timeBlockEntity: TimeBlockEntity) =
        localTimeBlockDataSource.insert(timeBlockEntity)

    suspend fun update(timeBlockEntity: TimeBlockEntity) =
        localTimeBlockDataSource.update(timeBlockEntity)

    suspend fun update(id: Int, activated: Boolean) =
        localTimeBlockDataSource.update(id, activated)

    suspend fun deleteAll() = localTimeBlockDataSource.deleteAll()
}