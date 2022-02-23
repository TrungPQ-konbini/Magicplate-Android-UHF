package com.konbini.magicplateuhf.data.remote.product

import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.data.remote.base.BaseDataSource
import com.konbini.magicplateuhf.data.remote.product.request.ProductsRequest
import javax.inject.Inject

class ProductRemoteDataSource@Inject constructor(
    private val productService: ProductService
): BaseDataSource() {
    suspend fun syncProducts(
        url: String,
        body: ProductsRequest
    ) = getResult {
        val api = AppSettings.APIs.ListAllProducts
        val path = "$url$api"
        productService.syncProducts(path, body.page, body.perPage, body.consumerKey, body.consumerSecret)
    }
}