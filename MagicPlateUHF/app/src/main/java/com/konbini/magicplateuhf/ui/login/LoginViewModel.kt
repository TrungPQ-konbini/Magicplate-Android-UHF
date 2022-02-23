package com.konbini.magicplateuhf.ui.login

import androidx.lifecycle.ViewModel
import com.konbini.magicplateuhf.AppSettings
import com.konbini.magicplateuhf.MainApplication
import com.konbini.magicplateuhf.R
import com.konbini.magicplateuhf.utils.Resource
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(): ViewModel() {
    companion object {
        const val TAG = "LoginViewModel"
    }

    private val resources = MainApplication.shared()

    private val adminPinCode = AppSettings.Machine.PinCode
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    suspend fun numberClicked(number: String) {
        val oldValue = _state.value.message
        val newValue = oldValue + number
        _state.value.status = Resource.Status.LOADING
        _state.emit(State(Resource.Status.LOADING, newValue))
    }

    suspend fun clearClicked() {
        _state.emit(State(Resource.Status.LOADING, ""))
    }

    suspend fun enterClicked() {
        val stateValue = if (adminPinCode == _state.value.message) {
            State(
                Resource.Status.SUCCESS,
                resources.getString(R.string.message_success_login)
            )
        } else {
            State(
                Resource.Status.ERROR,
                resources.getString(R.string.message_error_login)
            )
        }
        _state.emit(stateValue)
    }
}