package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.local.database.AppDatabase
import com.konbini.magicplateuhf.data.local.transaction.TransactionDao
import com.konbini.magicplateuhf.data.remote.transaction.TransactionRemoteDataSource
import com.konbini.magicplateuhf.data.remote.transaction.TransactionService
import com.konbini.magicplateuhf.data.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransactionModule {
    @Provides
    fun provideTransactionService(retrofit: Retrofit): TransactionService = retrofit.create(TransactionService::class.java)

    @Singleton
    @Provides
    fun provideTransactionRemoteDataSource(transactionService: TransactionService) = TransactionRemoteDataSource(transactionService)

    @Singleton
    @Provides
    fun provideTransactionDao(db: AppDatabase) = db.transactionDao()

    @Singleton
    @Provides
    fun provideTransactionRepository(
        localDataSource: TransactionDao,
        remoteDataSource: TransactionRemoteDataSource
    ) = TransactionRepository(localDataSource, remoteDataSource)
}