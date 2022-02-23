package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.local.database.AppDatabase
import com.konbini.magicplateuhf.data.local.offlineData.OfflineDataDao
import com.konbini.magicplateuhf.data.local.transaction.TransactionDao
import com.konbini.magicplateuhf.data.remote.transaction.TransactionRemoteDataSource
import com.konbini.magicplateuhf.data.repository.OfflineDataRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfflineDataModule {
    @Singleton
    @Provides
    fun provideOfflineDataDao(db: AppDatabase) = db.offlineDataDao()

    @Singleton
    @Provides
    fun provideOfflineDataRepository(
        localOfflineDataSource: OfflineDataDao,
        localTransactionDataSource: TransactionDao,
        transactionRemoteDataSource: TransactionRemoteDataSource
    ) = OfflineDataRepository(
        localOfflineDataSource,
        localTransactionDataSource,
        transactionRemoteDataSource
    )
}