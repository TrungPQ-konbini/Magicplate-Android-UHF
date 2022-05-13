package com.konbini.magicplateuhf.data.local.user

import androidx.room.*
import com.konbini.magicplateuhf.data.entities.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun getAll() : List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getSingleById(id: Int): UserEntity

    @Query("SELECT * FROM users WHERE ccwId1 = :ccwId1")
    suspend fun getSingleByCcwId1(ccwId1: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userEntity: UserEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(userEntity: UserEntity)

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteSingleById(id: Int)
}