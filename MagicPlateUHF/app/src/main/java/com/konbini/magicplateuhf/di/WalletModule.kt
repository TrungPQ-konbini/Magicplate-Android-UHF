package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.remote.wallet.WalletRemoteDataSource
import com.konbini.magicplateuhf.data.remote.wallet.WalletService
import com.konbini.magicplateuhf.data.repository.WalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WalletModule {
    @Provides
    fun provideWalletService(retrofit: Retrofit): WalletService = retrofit.create(WalletService::class.java)

    @Singleton
    @Provides
    fun provideWalletRemoteDataSource(walletService: WalletService) = WalletRemoteDataSource(walletService)

    @Singleton
    @Provides
    fun provideWalletRepository(remoteDataSource: WalletRemoteDataSource
    ) = WalletRepository(remoteDataSource)
}