package com.konbini.magicplateuhf.data.repository

import com.konbini.magicplateuhf.data.entities.ProductEntity
import com.konbini.magicplateuhf.data.local.product.ProductDao
import com.konbini.magicplateuhf.data.remote.product.ProductRemoteDataSource
import com.konbini.magicplateuhf.data.remote.product.request.ProductsRequest
import javax.inject.Inject

class ProductRepository @Inject constructor(
    private val localProductDataSource: ProductDao,
    private val remoteDataSource: ProductRemoteDataSource
) {
    suspend fun syncProducts(url: String, body: ProductsRequest) = remoteDataSource.syncProducts(url, body)

    suspend fun getAll() = localProductDataSource.getAll()

    suspend fun insertAll(productsEntity: List<ProductEntity>) = localProductDataSource.insertAll(productsEntity)

    suspend fun insert(productEntity: ProductEntity) = localProductDataSource.insert(productEntity)

    suspend fun deleteAll() = localProductDataSource.deleteAll()
}