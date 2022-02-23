package com.konbini.magicplateuhf.ui.category

import androidx.lifecycle.ViewModel
import com.konbini.magicplateuhf.data.entities.CategoryEntity
import com.konbini.magicplateuhf.data.repository.CategoryRepository
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    companion object {
        const val TAG = "CategoryViewModel"
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    suspend fun getAll() = categoryRepository.getAll()

    suspend fun update(categoryEntity: CategoryEntity) = categoryRepository.update(categoryEntity)
}