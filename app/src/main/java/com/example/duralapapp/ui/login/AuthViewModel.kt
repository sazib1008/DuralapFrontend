package com.example.duralapapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.model.AuthResponse
import com.example.duralapapp.data.model.LoginRequest
import com.example.duralapapp.data.model.UserCreateRequest
import com.example.duralapapp.data.repository.AuthRepository
import com.example.duralapapp.data.network.OfflineModeException
import com.example.duralapapp.data.network.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val bio: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val authResponse: AuthResponse? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    fun onUsernameChange(username: String) {
        _state.value = _state.value.copy(
            username = username,
            error = null
        )
    }

    fun onEmailChange(email: String) {
        _state.value = _state.value.copy(
            email = email,
            error = null
        )
    }

    fun onPasswordChange(password: String) {
        _state.value = _state.value.copy(
            password = password,
            error = null
        )
    }

    fun onFullNameChange(fullName: String) {
        _state.value = _state.value.copy(
            fullName = fullName,
            error = null
        )
    }

    fun onBioChange(bio: String) {
        _state.value = _state.value.copy(
            bio = bio,
            error = null
        )
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        _state.value = _state.value.copy(
            phoneNumber = phoneNumber,
            error = null
        )
    }

    fun login() {
        val current = _state.value
        val email = current.email.trim()
        val password = current.password

        if (email.isBlank() || password.isBlank()) {
            _state.value = current.copy(
                error = "Email and password required"
            )
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(
                isLoading = true,
                error = null
            )

            authRepository.login(LoginRequest(email, password))
                .onSuccess { data ->
                    tokenManager.saveTokens(
                        accessToken = data.accessToken,
                        refreshToken = data.refreshToken,
                        tokenType = data.tokenType,
                        expiresIn = data.expiresIn,
                        userId = data.user.id
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        authResponse = data,
                        error = null
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = if (e is OfflineModeException) null else e.message ?: "Login failed"
                    )
                }
        }
    }

    fun register() {
        val current = _state.value
        val username = current.username.trim()
        val email = current.email.trim()
        val password = current.password
        val fullName = current.fullName.trim().ifBlank { null }
        val bio = current.bio.trim().ifBlank { null }
        val phoneNumber = current.phoneNumber.trim().ifBlank { null }

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _state.value = current.copy(
                error = "Username, email and password are required"
            )
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(
                isLoading = true,
                error = null
            )

            val request = UserCreateRequest(
                username = username,
                email = email,
                password = password,
                fullName = fullName,
                bio = bio,
                phoneNumber = phoneNumber
            )

            authRepository.register(request)
                .onSuccess {
                    // Auto login after successful registration
                    login()
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = if (e is OfflineModeException) null else e.message ?: "Registration failed"
                    )
                }
        }
    }
}