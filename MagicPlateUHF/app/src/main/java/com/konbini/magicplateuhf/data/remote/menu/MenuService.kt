package com.konbini.magicplateuhf.data.remote.menu

import com.konbini.magicplateuhf.data.remote.menu.response.MenuResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface MenuService {
    @GET
    suspend fun syncMenus(
        @Url url: String
    ): Response<MenuResponse>
}