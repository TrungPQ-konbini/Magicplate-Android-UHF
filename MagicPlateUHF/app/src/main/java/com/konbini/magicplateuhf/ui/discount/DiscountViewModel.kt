package com.konbini.magicplateuhf.ui.discount

import androidx.lifecycle.ViewModel
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DiscountViewModel @Inject constructor(): ViewModel() {
    companion object {
        const val TAG = "DiscountViewModel"
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state
}