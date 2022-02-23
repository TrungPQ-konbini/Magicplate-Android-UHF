package com.konbini.magicplateuhf.ui.timeBlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.konbini.magicplateuhf.data.entities.TimeBlockEntity
import com.konbini.magicplateuhf.data.repository.TimeBlockRepository
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimeBlockViewModel @Inject constructor(
    private val timeBlockRepository: TimeBlockRepository
) : ViewModel() {
    companion object {
        const val TAG = "TimeBlockViewModel"
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    suspend fun getAll() = timeBlockRepository.getAll()

    fun insert(timeBlockEntity: TimeBlockEntity) = viewModelScope.launch {
        timeBlockRepository.insert(timeBlockEntity)
    }

    fun update(timeBlockEntity: TimeBlockEntity) = viewModelScope.launch {
        timeBlockRepository.update(timeBlockEntity)
    }

    fun update(id: Int, activated: Boolean) = viewModelScope.launch {
        timeBlockRepository.update(id, activated)
    }
}