package com.konbini.magicplateuhf.ui.users

import androidx.lifecycle.ViewModel
import com.konbini.magicplateuhf.data.entities.UserEntity
import com.konbini.magicplateuhf.data.repository.UserRepository
import com.konbini.magicplateuhf.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userRepository: UserRepository
): ViewModel() {
    companion object {
        const val TAG = "UsersViewModel"
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    suspend fun getAll() = userRepository.getAll()

    suspend fun update(userEntity: UserEntity) = userRepository.update(userEntity)
}