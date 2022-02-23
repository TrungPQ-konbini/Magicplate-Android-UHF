package com.konbini.magicplateuhf.data.local.menu

import androidx.room.*
import com.konbini.magicplateuhf.data.entities.MenuEntity

@Dao
interface MenuDao {
    @Query("SELECT * FROM menus")
    suspend fun getAll() : List<MenuEntity>

    @Query("SELECT * FROM menus WHERE menu_date LIKE :menuDate")
    suspend fun getMenuByDate(menuDate: String) : List<MenuEntity>

    @Query("SELECT * FROM menus WHERE id = :id")
    suspend fun getSingleById(id: Long): MenuEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(menuEntity: MenuEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(menuEntity: MenuEntity)

    @Query("DELETE FROM menus")
    suspend fun deleteAll()

    @Query("DELETE FROM menus WHERE id = :id")
    suspend fun deleteSingleById(id: Long)
}