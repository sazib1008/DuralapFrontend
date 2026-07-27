package com.example.duralapapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.local.AuthEventBus
import com.example.duralapapp.data.local.SessionManager
import com.example.duralapapp.data.model.AuthEvent
import com.example.duralapapp.data.model.UserResponse
import com.example.duralapapp.data.model.UserUpdateRequest
import com.example.duralapapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: UserResponse) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            userRepository.getCurrentUserProfile()
                .onSuccess { user ->
                    _uiState.value = ProfileUiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = ProfileUiState.Error(
                        error.localizedMessage ?: "Failed to load profile"
                    )
                }
        }
    }

    fun updateProfile(fullName: String?, bio: String?) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) return

        viewModelScope.launch {
            val userId = currentState.user.id
            val request = UserUpdateRequest(fullName = fullName, bio = bio)

            userRepository.updateUserProfile(userId, request)
                .onSuccess { updatedUser ->
                    _uiState.value = ProfileUiState.Success(updatedUser)
                    _updateMessage.value = "Profile updated successfully!"
                }
                .onFailure { error ->
                    _updateMessage.value = error.localizedMessage ?: "Failed to update profile"
                }
        }
    }

    fun clearUpdateMessage() {
        _updateMessage.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            sessionManager.clearSessionAndLogout("User signed out")
            AuthEventBus.emit(AuthEvent.Logout("User signed out"))
        }
    }
}
