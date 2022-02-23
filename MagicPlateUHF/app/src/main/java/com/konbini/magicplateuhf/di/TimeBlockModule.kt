package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.local.database.AppDatabase
import com.konbini.magicplateuhf.data.local.timeBlock.TimeBlockDao
import com.konbini.magicplateuhf.data.remote.timeBlock.TimeBlockRemoteDataSource
import com.konbini.magicplateuhf.data.remote.timeBlock.TimeBlockService
import com.konbini.magicplateuhf.data.repository.TimeBlockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeBlockModule {
    @Provides
    fun provideTimeBlockService(retrofit: Retrofit): TimeBlockService =
        retrofit.create(TimeBlockService::class.java)

    @Singleton
    @Provides
    fun provideTimeBlockRemoteDataSource(timeBlockService: TimeBlockService) =
        TimeBlockRemoteDataSource(timeBlockService)

    @Singleton
    @Provides
    fun provideTimeBlockDao(db: AppDatabase) = db.timeBlockDao()

    @Singleton
    @Provides
    fun provideTimeBlockRepository(
        localTimeBlockDataSource: TimeBlockDao,
        remoteDataSource: TimeBlockRemoteDataSource
    ) = TimeBlockRepository(localTimeBlockDataSource, remoteDataSource)
}