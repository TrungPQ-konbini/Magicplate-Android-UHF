package com.konbini.magicplateuhf.ui.transaction

import androidx.lifecycle.ViewModel
import com.konbini.magicplateuhf.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    companion object {
        const val TAG = "TransactionViewModel"
    }

    suspend fun getAll() = transactionRepository.getAll()

    suspend fun getAllToday(startToday: Long, endToday: Long) =
        transactionRepository.getAllToday(startToday, endToday)

    suspend fun getAllByRangeDate(fromDate: Long, toDate: Long) =
        transactionRepository.getAllByRangeDate(fromDate, toDate)

    suspend fun getSingleByUuid(uuid: String) =
        transactionRepository.getSingleByUuid(uuid)
}