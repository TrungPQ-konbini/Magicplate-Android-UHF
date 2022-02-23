package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.local.database.AppDatabase
import com.konbini.magicplateuhf.data.local.menu.MenuDao
import com.konbini.magicplateuhf.data.remote.menu.MenuRemoteDataSource
import com.konbini.magicplateuhf.data.remote.menu.MenuService
import com.konbini.magicplateuhf.data.repository.MenuRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MenuModule {
    @Provides
    fun provideMenuService(retrofit: Retrofit): MenuService =
        retrofit.create(MenuService::class.java)

    @Singleton
    @Provides
    fun provideMenuRemoteDataSource(menuService: MenuService) = MenuRemoteDataSource(menuService)

    @Singleton
    @Provides
    fun provideMenuDao(db: AppDatabase) = db.menuDao()

    @Singleton
    @Provides
    fun provideMenuRepository(
        localMenuDataSource: MenuDao,
        remoteDataSource: MenuRemoteDataSource
    ) = MenuRepository(localMenuDataSource, remoteDataSource)
}