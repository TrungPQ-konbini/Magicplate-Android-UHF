package com.konbini.magicplateuhf.data.local.transaction

import androidx.room.*
import com.konbini.magicplateuhf.data.entities.TransactionEntity

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE payment_time >= :startToday AND payment_time <= :endToday")
    suspend fun getAllToday(startToday: Long, endToday: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE payment_time >= :fromDate AND payment_time <= :toDate")
    suspend fun getAllByRangeDate(fromDate: Long, toDate: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE uuid = :uuid")
    suspend fun getSingleByUuid(uuid: String): TransactionEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transactionEntity: TransactionEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(transactionEntity: TransactionEntity)

    @Query("UPDATE transactions SET syncId = :syncId, syncedDate = :syncedDate WHERE uuid = :uuid")
    suspend fun update(uuid: String?, syncId: Int?, syncedDate: String?)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteSingleById(id: Long)

    @Query("DELETE FROM transactions WHERE uuid = :uuid")
    fun deleteSingleByUuid(uuid: String)
}