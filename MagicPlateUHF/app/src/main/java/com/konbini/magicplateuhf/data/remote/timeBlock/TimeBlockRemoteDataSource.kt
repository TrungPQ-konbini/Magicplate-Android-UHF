package com.konbini.magicplateuhf.data.remote.timeBlock

import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import javax.inject.Inject

class TimeBlockRemoteDataSource@Inject constructor(
    private val timeBlockService: TimeBlockService
): BaseDataSource() {
    suspend fun syncTimeBlocks(
        url: String
    ) = getResult {
        val api = AppSettings.APIs.ListAllTimeBlock
        val path = "$url$api"
        timeBlockService.syncTimeBlocks(path)
    }
}