package com.konbini.magicplateuhf.data.repository

import com.konbini.magicplateuhf.data.entities.UserEntity
import com.konbini.magicplateuhf.data.local.user.UserDao
import com.konbini.magicplateuhf.data.remote.user.UserRemoteDataSource
import com.konbini.magicplateuhf.data.remote.user.request.GetAllUserRequest
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val localCategoryDataSource: UserDao,
    private val remoteDataSource: UserRemoteDataSource
) {
    suspend fun getAllUsers(url: String, body: GetAllUserRequest) =
        remoteDataSource.getAllUsers(url, body)

    suspend fun getAll() = localCategoryDataSource.getAll()

    suspend fun getSingleByCcwId1(ccwId1: String) = localCategoryDataSource.getSingleByCcwId1(ccwId1)

    suspend fun insertAll(users: List<UserEntity>) = localCategoryDataSource.insertAll(users)

    suspend fun insert(userEntity: UserEntity) = localCategoryDataSource.insert(userEntity)

    suspend fun update(userEntity: UserEntity) = localCategoryDataSource.update(userEntity)

    suspend fun deleteAll() = localCategoryDataSource.deleteAll()
}