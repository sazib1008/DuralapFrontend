package com.example.duralab.data.repository

import com.example.duralab.data.model.AuthResponse
import com.example.duralab.data.model.LoginRequest
import com.example.duralab.data.model.RegisterRequest
import com.example.duralab.data.remote.AuthApi
import com.example.duralab.util.TokenManager
import com.example.duralab.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    suspend fun login(loginRequest: LoginRequest): Flow<UiState<AuthResponse>> = flow {
        emit(UiState.Loading)
        try {
            val response = authApi.login(loginRequest)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveToken(authResponse.token)
                tokenManager.saveRefreshToken(authResponse.refreshToken)
                emit(UiState.Success(authResponse))
            } else {
                emit(UiState.Error(response.message() ?: "Login failed"))
            }
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "An unknown error occurred"))
        }
    }

    suspend fun register(registerRequest: RegisterRequest): Flow<UiState<AuthResponse>> = flow {
        emit(UiState.Loading)
        try {
            val response = authApi.register(registerRequest)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveToken(authResponse.token)
                tokenManager.saveRefreshToken(authResponse.refreshToken)
                emit(UiState.Success(authResponse))
            } else {
                emit(UiState.Error(response.message() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "An unknown error occurred"))
        }
    }

    fun logout() {
        tokenManager.clear()
    }

    fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }
}
