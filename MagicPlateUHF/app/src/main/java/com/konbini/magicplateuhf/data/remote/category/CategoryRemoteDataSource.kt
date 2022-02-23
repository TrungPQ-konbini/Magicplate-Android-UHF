package com.konbini.magicplateuhf.data.remote.category

import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import com.konbini.magicplateuhf.data.remote.category.request.CategoriesRequest
import javax.inject.Inject

class CategoryRemoteDataSource @Inject constructor(
    private val categoryService: CategoryService
) : BaseDataSource() {
    suspend fun syncCategories(
        url: String,
        body: CategoriesRequest
    ) = getResult {
        val api = AppSettings.APIs.ListAllProductCategories
        val path = "$url$api"
        categoryService.syncCategories(path, body.page, body.perPage, body.consumerKey, body.consumerSecret)
    }
}