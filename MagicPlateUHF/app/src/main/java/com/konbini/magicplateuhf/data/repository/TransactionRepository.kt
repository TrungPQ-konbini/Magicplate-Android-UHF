package com.konbini.magicplateuhf.data.repository

import com.konbini.magicplateuhf.data.entities.TransactionEntity
import com.konbini.magicplateuhf.data.local.transaction.TransactionDao
import com.konbini.magicplateuhf.data.remote.transaction.TransactionRemoteDataSource
import com.konbini.magicplateuhf.data.remote.transaction.request.OrderRequest
import com.konbini.magicplateuhf.data.remote.transaction.request.SubmitTransactionRequest
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val localDataSource: TransactionDao,
    private val remoteDataSource: TransactionRemoteDataSource
) {
    suspend fun getLastTransactionId() = localDataSource.getLastTransactionId()

    suspend fun getAll() = localDataSource.getAll()

    suspend fun getAllToday(startToday: Long, endToday: Long) =
        localDataSource.getAllToday(startToday, endToday)

    suspend fun getAllByRangeDate(fromDate: Long, toDate: Long) =
        localDataSource.getAllByRangeDate(fromDate, toDate)

    suspend fun getSingleByUuid(uuid: String) =
        localDataSource.getSingleByUuid(uuid)

    suspend fun insert(transactionEntity: TransactionEntity) =
        localDataSource.insert(transactionEntity)

    suspend fun update(transactionEntity: TransactionEntity) =
        localDataSource.update(transactionEntity)

    suspend fun update(uuid: String?, syncId: Int?, syncedDate: String?) =
        localDataSource.update(uuid, syncId, syncedDate)

    suspend fun deleteSingleByUuid(uuid: String) = localDataSource.deleteSingleByUuid(uuid)

    suspend fun createAnOrder(
        url: String,
        bodyRequest: OrderRequest
    ) = remoteDataSource.createAnOrder(url, bodyRequest)

    suspend fun submitTransaction(
        url: String,
        bodyRequest: SubmitTransactionRequest
    ) = remoteDataSource.submitTransaction(url, bodyRequest)
}