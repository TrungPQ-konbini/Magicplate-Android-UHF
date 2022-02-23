package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.local.database.AppDatabase
import com.konbini.magicplateuhf.data.local.product.ProductDao
import com.konbini.magicplateuhf.data.remote.product.ProductRemoteDataSource
import com.konbini.magicplateuhf.data.remote.product.ProductService
import com.konbini.magicplateuhf.data.repository.ProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProductModule {
    @Provides
    fun provideProductService(retrofit: Retrofit): ProductService = retrofit.create(ProductService::class.java)

    @Singleton
    @Provides
    fun provideProductRemoteDataSource(productService: ProductService) = ProductRemoteDataSource(productService)

    @Singleton
    @Provides
    fun provideProductDao(db: AppDatabase) = db.productDao()

    @Singleton
    @Provides
    fun provideProductRepository(
        localProductDataSource: ProductDao,
        remoteDataSource: ProductRemoteDataSource
    ) = ProductRepository(localProductDataSource, remoteDataSource)
}