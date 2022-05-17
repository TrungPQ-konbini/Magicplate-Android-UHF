package com.konbini.magicplateuhf.data.local.offlineData

import androidx.room.*
import com.konbini.magicplateuhf.data.entities.OfflineDataEntity

@Dao
interface OfflineDataDao {
    @Query("SELECT * FROM offline_data")
    suspend fun getAll() : List<OfflineDataEntity>

    @Query("SELECT * FROM offline_data where is_synced = 0")
    suspend fun getNotSyncedData(): List<OfflineDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(offlineDataEntity: OfflineDataEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(offlineDataEntity: OfflineDataEntity)

    @Query("UPDATE offline_data SET is_synced = :isSynced WHERE uuid = :uuid")
    suspend fun update(uuid: String?, isSynced: Boolean)
}