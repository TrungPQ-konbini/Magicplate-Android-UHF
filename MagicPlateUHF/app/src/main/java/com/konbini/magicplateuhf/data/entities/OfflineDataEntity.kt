package com.konbini.magicplateuhf.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_data")
data class OfflineDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "data_type") val dataType: String,
    @ColumnInfo(name = "json") val json: String,
    @ColumnInfo(name = "created_date") var createdDate: String,
    @ColumnInfo(name = "is_synced") var isSynced: Boolean
)
