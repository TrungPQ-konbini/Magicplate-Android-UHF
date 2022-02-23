package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.local.category.CategoryDao
import com.konbini.magicplateuhf.data.local.database.AppDatabase
import com.konbini.magicplateuhf.data.remote.category.CategoryRemoteDataSource
import com.konbini.magicplateuhf.data.remote.category.CategoryService
import com.konbini.magicplateuhf.data.repository.CategoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CategoryModule {
    @Provides
    fun provideCategoryService(retrofit: Retrofit): CategoryService =
        retrofit.create(CategoryService::class.java)

    @Singleton
    @Provides
    fun provideCategoryRemoteDataSource(categoryService: CategoryService) =
        CategoryRemoteDataSource(categoryService)

    @Singleton
    @Provides
    fun provideCategoryDao(db: AppDatabase) = db.categoryDao()

    @Singleton
    @Provides
    fun provideCategoryRepository(
        localCategoryDataSource: CategoryDao,
        remoteDataSource: CategoryRemoteDataSource
    ) =
        CategoryRepository(localCategoryDataSource, remoteDataSource)
}