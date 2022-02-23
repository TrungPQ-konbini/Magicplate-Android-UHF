package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.local.database.AppDatabase
import com.konbini.magicplateuhf.data.local.plateModel.PlateModelDao
import com.konbini.magicplateuhf.data.remote.plateModel.PlateModelRemoteDataSource
import com.konbini.magicplateuhf.data.remote.plateModel.PlateModelService
import com.konbini.magicplateuhf.data.repository.PlateModelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlateModelModule {
    @Provides
    fun providePlateModelService(retrofit: Retrofit): PlateModelService =
        retrofit.create(PlateModelService::class.java)

    @Singleton
    @Provides
    fun providePlateModelRemoteDataSource(plateModelService: PlateModelService) =
        PlateModelRemoteDataSource(plateModelService)

    @Singleton
    @Provides
    fun providePlateModelDao(db: AppDatabase) = db.plateModelDao()

    @Singleton
    @Provides
    fun providePlateModelRepository(
        localPlateModelDataSource: PlateModelDao,
        remoteDataSource: PlateModelRemoteDataSource
    ) = PlateModelRepository(localPlateModelDataSource, remoteDataSource)
}