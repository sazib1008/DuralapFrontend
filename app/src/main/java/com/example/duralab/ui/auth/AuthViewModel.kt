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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginFormState(
    val usernameOrEmail: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    val biometricHelper: com.example.duralab.util.BiometricHelper
) : ViewModel() {

    private val _authState = MutableStateFlow<UiState<AuthResponse>>(UiState.Success(AuthResponse("", "", 0, com.example.duralab.data.model.UserResponse("", "", "")))) // Initial state could be something else
    // Actually, setting initial state to something more neutral or Loading if checking login
    private val _loginState = MutableStateFlow<UiState<AuthResponse>?>(null)
    val loginState: StateFlow<UiState<AuthResponse>?> = _loginState.asStateFlow()

    private val _loginFormState = MutableStateFlow(LoginFormState())
    val loginFormState: StateFlow<LoginFormState> = _loginFormState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<com.example.duralab.data.model.UserResponse>?>(null)
    val registerState: StateFlow<UiState<com.example.duralab.data.model.UserResponse>?> = _registerState.asStateFlow()

    fun onUsernameOrEmailChange(value: String) {
        _loginFormState.value = _loginFormState.value.copy(usernameOrEmail = value)
    }

    fun onPasswordChange(value: String) {
        _loginFormState.value = _loginFormState.value.copy(password = value)
    }

    fun onPasswordVisibilityChange(visible: Boolean) {
        _loginFormState.value = _loginFormState.value.copy(passwordVisible = visible)
    }

    fun login(usernameOrEmail: String, password: String) {
        viewModelScope.launch {
            repository.login(LoginRequest(usernameOrEmail, password)).collect {
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
        viewModelScope.launch {
            repository.logout().collect {
                // Optional: Update a state if needed
            }
        }
    }

    fun isLoggedIn() = repository.isLoggedIn()
}
