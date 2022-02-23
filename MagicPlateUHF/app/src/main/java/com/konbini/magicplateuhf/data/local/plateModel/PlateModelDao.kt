package com.konbini.magicplateuhf.data.local.plateModel

import androidx.room.*
import com.konbini.magicplateuhf.data.entities.PlateModelEntity

@Dao
interface PlateModelDao {
    @Query("SELECT * FROM plateModels")
    suspend fun getAll() : List<PlateModelEntity>

    @Query("SELECT * FROM plateModels WHERE id = :id")
    suspend fun getSingleById(id: Int): PlateModelEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plateModelEntity: PlateModelEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(plateModelEntity: PlateModelEntity)

    @Query("DELETE FROM plateModels")
    suspend fun deleteAll()

    @Query("DELETE FROM plateModels WHERE id = :id")
    suspend fun deleteSingleById(id: Int)
}