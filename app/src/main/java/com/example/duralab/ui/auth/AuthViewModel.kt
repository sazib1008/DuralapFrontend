package com.example.duralab.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralab.data.model.AuthResponse
import com.example.duralab.data.model.LoginRequest
import com.example.duralab.data.model.RegisterRequest
import com.example.duralab.data.repository.AuthRepository
import com.example.duralab.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    val biometricHelper: com.example.duralab.util.BiometricHelper
) : ViewModel() {

    private val _authState = MutableStateFlow<UiState<AuthResponse>>(UiState.Success(AuthResponse("", "", com.example.duralab.data.model.UserDto("", "", "", null)))) // Initial state could be something else
    // Actually, setting initial state to something more neutral or Loading if checking login
    private val _loginState = MutableStateFlow<UiState<AuthResponse>?>(null)
    val loginState: StateFlow<UiState<AuthResponse>?> = _loginState

    private val _registerState = MutableStateFlow<UiState<AuthResponse>?>(null)
    val registerState: StateFlow<UiState<AuthResponse>?> = _registerState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            repository.login(LoginRequest(email, password)).collect {
                _loginState.value = it
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            repository.register(RegisterRequest(username, email, password)).collect {
                _registerState.value = it
            }
        }
    }

    fun logout() {
        repository.logout()
    }

    fun isLoggedIn() = repository.isLoggedIn()
}
