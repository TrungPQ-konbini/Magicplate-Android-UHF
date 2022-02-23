package com.konbini.magicplateuhf.data.local.category

import androidx.room.*
import com.konbini.magicplateuhf.data.entities.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun getAll() : List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getSingleById(id: Int): CategoryEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoryEntity: CategoryEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(categoryEntity: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteSingleById(id: Long)
}