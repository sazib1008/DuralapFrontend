package com.example.duralapapp.data.local

import com.example.duralapapp.data.network.TokenManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenValidator @Inject constructor(
    private val tokenManager: TokenManager
) {
    suspend fun isTokenValid(): Boolean {
        val accessToken = tokenManager.accessToken.first()
        if (accessToken.isNullOrEmpty()) return false
        return !tokenManager.isAccessTokenExpired()
    }

    suspend fun hasRefreshToken(): Boolean {
        val refreshToken = tokenManager.refreshToken.first()
        return !refreshToken.isNullOrEmpty()
    }

    suspend fun isLoggedIn(): Boolean = tokenManager.hasTokens()

    suspend fun isTokenExpired(): Boolean {
        val accessToken = tokenManager.accessToken.first()
        if (accessToken.isNullOrEmpty()) return true
        return tokenManager.isAccessTokenExpired()
    }
}
