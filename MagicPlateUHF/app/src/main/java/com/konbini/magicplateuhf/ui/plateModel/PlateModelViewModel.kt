package com.konbini.magicplateuhf.ui.plateModel

import androidx.lifecycle.ViewModel
import com.konbini.magicplateuhf.data.repository.PlateModelRepository
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlateModelViewModel @Inject constructor(
    private val plateModelRepository: PlateModelRepository
) : ViewModel() {
    companion object {
        const val TAG = "PlateModelViewModel"
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    suspend fun getAll() = plateModelRepository.getAll()
}