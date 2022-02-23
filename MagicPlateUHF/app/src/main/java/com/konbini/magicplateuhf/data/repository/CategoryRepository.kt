package com.konbini.magicplateuhf.data.repository

import com.konbini.magicplateuhf.data.entities.CategoryEntity
import com.konbini.magicplateuhf.data.local.category.CategoryDao
import com.konbini.magicplateuhf.data.remote.category.CategoryRemoteDataSource
import com.konbini.magicplateuhf.data.remote.category.request.CategoriesRequest
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val localCategoryDataSource: CategoryDao,
    private val remoteDataSource: CategoryRemoteDataSource
) {
    suspend fun syncCategories(url: String, body: CategoriesRequest) =
        remoteDataSource.syncCategories(url, body)

    suspend fun getAll() = localCategoryDataSource.getAll()

    suspend fun insert(categoryEntity: CategoryEntity) = localCategoryDataSource.insert(categoryEntity)

    suspend fun update(categoryEntity: CategoryEntity) = localCategoryDataSource.update(categoryEntity)

    suspend fun deleteAll() = localCategoryDataSource.deleteAll()
}