package com.example.duralab.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralab.data.model.UserResponse
import com.example.duralab.data.model.UserUpdateRequest
import com.example.duralab.data.repository.AuthRepository
import com.example.duralab.data.repository.UserRepository
import com.example.duralab.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserResponse? = null,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val editUsername: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        val userId = userRepository.getCurrentUserId()
        if (userId != null) {
            viewModelScope.launch {
                userRepository.getUserProfile(userId).collect { result ->
                    when (result) {
                        is UiState.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                        }
                        is UiState.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                userProfile = result.data,
                                editUsername = result.data.username
                            )
                        }
                        is UiState.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                error = "User ID not found. Please log in again."
            )
        }
    }

    fun toggleEditMode() {
        val currentState = _uiState.value
        if (currentState.isEditMode) {
            // Cancel edit mode
            _uiState.value = currentState.copy(
                isEditMode = false,
                editUsername = currentState.userProfile?.username ?: ""
            )
        } else {
            _uiState.value = currentState.copy(isEditMode = true)
        }
    }

    fun updateEditUsername(newName: String) {
        _uiState.value = _uiState.value.copy(editUsername = newName)
    }

    fun saveProfile() {
        val currentState = _uiState.value
        val userId = userRepository.getCurrentUserId()
        
        if (userId != null && currentState.editUsername.isNotBlank()) {
            viewModelScope.launch {
                val request = UserUpdateRequest(username = currentState.editUsername.trim())
                userRepository.updateUserProfile(userId, request).collect { result ->
                    when (result) {
                        is UiState.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                        }
                        is UiState.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                userProfile = result.data,
                                isEditMode = false,
                                editUsername = result.data.username
                            )
                        }
                        is UiState.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout().collect {
                // Logout is handled
            }
        }
    }
    
    fun clearError() {
      _uiState.value = _uiState.value.copy(error = null)
    }
}
