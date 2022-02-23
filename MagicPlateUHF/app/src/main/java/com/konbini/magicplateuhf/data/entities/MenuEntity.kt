package com.konbini.magicplateuhf.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.konbini.magicplateuhf.base.BaseEntity

@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "menu_date") val menuDate: String,
    @ColumnInfo(name = "time_block_id") val timeBlockId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "plate_model_id") val plateModelId: String,
    @ColumnInfo(name = "price") val price: String,
    @ColumnInfo(name = "product_name") var productName: String,
    @ColumnInfo(name = "plate_model_name") val plateModelName: String,
    @ColumnInfo(name = "plate_model_code") val plateModelCode: String,
    @ColumnInfo(name = "time_block_title") val timeBlockTitle: String,
    @ColumnInfo(name = "quantity") var quantity: Int,
    @ColumnInfo(name = "options") var options: String
) : BaseEntity()
