package com.konbini.magicplateuhf.ui.options

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OptionsViewModel @Inject constructor() : ViewModel() {
    companion object {
        const val TAG = "OptionsViewModel"
    }
}