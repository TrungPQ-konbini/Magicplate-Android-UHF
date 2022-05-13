package com.konbini.magicplateuhf.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.konbini.magicplateuhf.base.BaseEntity

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "parent_id") val parentId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "categories") val categories: String,
    @ColumnInfo(name = "images") val images: String,
    @ColumnInfo(name = "barcode") val barcode: String,
    @ColumnInfo(name = "options") val options: String,
    @ColumnInfo(name = "quantity") var quantity: Int,
    @ColumnInfo(name = "price") val price: String,
    @ColumnInfo(name = "regular_price") val regularPrice: String,
    @ColumnInfo(name = "sale_price") val salePrice: String,
    @ColumnInfo(name = "menu_order") val menuOrder: Int
) : BaseEntity()
