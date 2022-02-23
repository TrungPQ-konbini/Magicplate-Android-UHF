package com.konbini.magicplateuhf.ui.writeTags

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WriteTagsViewModel @Inject constructor() : ViewModel() {
    companion object {
        const val TAG = "WriteTagsViewModel"
    }
}