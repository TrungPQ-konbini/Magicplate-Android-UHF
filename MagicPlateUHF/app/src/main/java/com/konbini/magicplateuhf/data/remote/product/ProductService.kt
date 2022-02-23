package com.konbini.magicplateuhf.data.remote.product

import com.konbini.magicplateuhf.data.remote.product.response.ProductsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface ProductService {
    @GET
    suspend fun syncProducts(
        @Url url: String,
        @Query("page") page: Int?,
        @Query("per_page") per_page: Int?,
        @Query("consumer_key") consumer_key: String?,
        @Query("consumer_secret") consumer_secret: String?
    ): Response<ProductsResponse>
}