package com.konbini.magicplateuhf.data.remote.timeBlock

import com.konbini.magicplateuhf.data.remote.timeBlock.response.TimeBlockResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface TimeBlockService {
    @GET
    suspend fun syncTimeBlocks(
        @Url url: String
    ): Response<TimeBlockResponse>
}