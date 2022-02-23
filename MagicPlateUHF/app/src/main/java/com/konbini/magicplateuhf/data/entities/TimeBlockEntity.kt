package com.konbini.magicplateuhf.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.konbini.magicplateuhf.base.BaseEntity

@Entity(tableName = "timeBlocks")
data class TimeBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "time_block_id") val timeBlockId: String,
    @ColumnInfo(name = "from_hour") var fromHour: String,
    @ColumnInfo(name = "to_hour") var toHour: String,
    @ColumnInfo(name = "time_block_title") var timeBlockTitle: String
) : BaseEntity()
