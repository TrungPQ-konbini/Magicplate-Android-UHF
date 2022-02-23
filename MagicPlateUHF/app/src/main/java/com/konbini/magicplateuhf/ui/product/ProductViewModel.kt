package com.konbini.magicplateuhf.ui.product

import androidx.lifecycle.ViewModel
import com.konbini.magicplateuhf.data.repository.CategoryRepository
import com.konbini.magicplateuhf.data.repository.ProductRepository
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository
) : ViewModel() {
    companion object {
        const val TAG = "ProductViewModel"
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    suspend fun getAll() = productRepository.getAll()

    suspend fun getAllCategories() = categoryRepository.getAll()
}