package com.konbini.magicplateuhf.ui.menu

import androidx.lifecycle.ViewModel
import com.konbini.magicplateuhf.data.repository.MenuRepository
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository
) : ViewModel() {
    companion object {
        const val TAG = "MenuViewModel"
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    suspend fun getAll() = menuRepository.getAll()
}