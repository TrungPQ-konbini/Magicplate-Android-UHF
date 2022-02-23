package com.konbini.magicplateuhf.data.local.timeBlock

import androidx.room.*
import com.konbini.magicplateuhf.data.entities.TimeBlockEntity

@Dao
interface TimeBlockDao {
    @Query("SELECT * FROM timeBlocks")
    suspend fun getAll() : List<TimeBlockEntity>

    @Query("SELECT * FROM timeBlocks WHERE id = :id")
    suspend fun getSingleById(id: Int): TimeBlockEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timeBlockEntity: TimeBlockEntity)

    @Update
    suspend fun update(timeBlockEntity: TimeBlockEntity)

    @Query("Update timeBlocks SET activated = :activated WHERE id = :id")
    suspend fun update(id: Int, activated: Boolean)

    @Query("DELETE FROM timeBlocks")
    suspend fun deleteAll()

    @Query("DELETE FROM timeBlocks WHERE id = :id")
    suspend fun deleteSingleById(id: Int)
}