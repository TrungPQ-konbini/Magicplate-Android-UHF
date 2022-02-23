package com.konbini.magicplateuhf.data.local.product

import androidx.room.*
import com.konbini.magicplateuhf.data.entities.ProductEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    suspend fun getAll() : List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getSingleById(id: Int): ProductEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productsEntity: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(productEntity: ProductEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(productEntity: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteSingleById(id: Int)
}