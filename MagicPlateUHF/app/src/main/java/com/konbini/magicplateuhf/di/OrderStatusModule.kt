package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.remote.orderStatus.OrderStatusDataSource
import com.konbini.magicplateuhf.data.remote.orderStatus.OrderStatusService
import com.konbini.magicplateuhf.data.repository.OrderStatusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OrderStatusModule {
    @Provides
    fun provideOrderStatusService(retrofit: Retrofit): OrderStatusService =
        retrofit.create(OrderStatusService::class.java)

    @Singleton
    @Provides
    fun provideOrderStatusDataSource(orderStatusService: OrderStatusService) = OrderStatusDataSource(orderStatusService)

    @Singleton
    @Provides
    fun provideOrderStatusRepository(
        remoteDataSource: OrderStatusDataSource
    ) = OrderStatusRepository(remoteDataSource)
}