package com.konbini.magicplateuhf.di

import com.konbini.magicplateuhf.data.local.database.AppDatabase
import com.konbini.magicplateuhf.data.local.user.UserDao
import com.konbini.magicplateuhf.data.remote.user.UserRemoteDataSource
import com.konbini.magicplateuhf.data.remote.user.UserService
import com.konbini.magicplateuhf.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserModule {
    @Provides
    fun provideUserService(retrofit: Retrofit): UserService = retrofit.create(UserService::class.java)

    @Singleton
    @Provides
    fun provideUserRemoteDataSource(userService: UserService) = UserRemoteDataSource(userService)

    @Singleton
    @Provides
    fun provideUserDao(db: AppDatabase) = db.userDao()

    @Singleton
    @Provides
    fun provideUserRepository(localUserDataSource: UserDao, remoteDataSource: UserRemoteDataSource
    ) = UserRepository(localUserDataSource, remoteDataSource)
}