package com.konbini.magicplateuhf.data.repository

import com.konbini.magicplateuhf.data.entities.MenuEntity
import com.konbini.magicplateuhf.data.local.menu.MenuDao
import com.konbini.magicplateuhf.data.remote.menu.MenuRemoteDataSource
import javax.inject.Inject

class MenuRepository @Inject constructor(
    private val localMenuDataSource: MenuDao,
    private val remoteDataSource: MenuRemoteDataSource
) {
    suspend fun syncMenus(url: String) =
        remoteDataSource.syncMenus(url)

    suspend fun getAll() = localMenuDataSource.getAll()

    suspend fun getMenuByDate(menuDate: String) = localMenuDataSource.getMenuByDate(menuDate)

    suspend fun insert(menuEntity: MenuEntity) =
        localMenuDataSource.insert(menuEntity)

    suspend fun deleteByMenuDate(menuDate: String) = localMenuDataSource.deleteByMenuDate(menuDate)

    suspend fun deleteAll() = localMenuDataSource.deleteAll()
}