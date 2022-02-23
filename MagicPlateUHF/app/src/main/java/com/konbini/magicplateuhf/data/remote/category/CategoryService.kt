package com.konbini.magicplateuhf.data.remote.category

import com.konbini.magicplateuhf.data.remote.category.response.CategoriesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface CategoryService {
    @GET
    suspend fun syncCategories(
        @Url url: String,
        @Query("page") page: Int?,
        @Query("per_page") per_page: Int?,
        @Query("consumer_key") consumer_key: String?,
        @Query("consumer_secret") consumer_secret: String?
    ): Response<CategoriesResponse>
}