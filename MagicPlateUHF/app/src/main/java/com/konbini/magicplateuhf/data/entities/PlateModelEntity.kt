package com.konbini.magicplateuhf.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.konbini.magicplateuhf.base.BaseEntity

@Entity(tableName = "plateModels")
data class PlateModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "plate_model_id") val plateModelId: String,
    @ColumnInfo(name = "plate_model_code") val plateModelCode: String,
    @ColumnInfo(name = "plate_model_title") val plateModelTitle: String,
    @ColumnInfo(name = "last_plate_serial") val lastPlateSerial: String
) : BaseEntity()
