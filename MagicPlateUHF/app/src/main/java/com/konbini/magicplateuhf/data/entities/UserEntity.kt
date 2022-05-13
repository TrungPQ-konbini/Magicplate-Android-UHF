package com.konbini.magicplateuhf.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val displayName: String,
    val roles: String,
    val ccwId1: String,
    val ccwId2: String,
    val ccwId3: String,
)
