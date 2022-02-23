package com.konbini.magicplateuhf.data.remote.menu

import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import javax.inject.Inject

class MenuRemoteDataSource@Inject constructor(
    private val menuService: MenuService
): BaseDataSource() {
    suspend fun syncMenus(
        url: String
    ) = getResult {
        val api = AppSettings.APIs.ListAllMenu
        val path = "$url$api"
        menuService.syncMenus(path)
    }
}