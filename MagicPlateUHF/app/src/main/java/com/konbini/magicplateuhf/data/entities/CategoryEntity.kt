package com.konbini.magicplateuhf.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.konbini.magicplateuhf.base.BaseEntity

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parent") val parent: Int,
    @ColumnInfo(name = "menu_order") val menuOrder: Int
) : BaseEntity()
