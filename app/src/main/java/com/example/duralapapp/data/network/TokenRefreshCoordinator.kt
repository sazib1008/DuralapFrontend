package com.example.duralapapp.data.network

import com.example.duralapapp.data.api.AuthApi
import com.example.duralapapp.data.local.SessionManager
import com.example.duralapapp.data.model.TokenRefreshRequest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TokenRefreshCoordinator @Inject constructor(
    @Named("refreshAuthApi")
    private val refreshAuthApi: AuthApi,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) {
    private val refreshMutex = Mutex()
    private val blacklistedRefreshTokens = mutableSetOf<String>()

    suspend fun refreshIfNeeded(forceRefresh: Boolean = false): Boolean {
        if (!forceRefresh && !tokenManager.isAccessTokenExpired()) {
            return true
        }

        return refreshMutex.withLock {
            if (!forceRefresh && !tokenManager.isAccessTokenExpired()) {
                return@withLock true
            }

            val refreshToken = tokenManager.refreshToken.firstOrNull()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.clearSessionAndLogout("Missing refresh token")
                return@withLock false
            }

            if (blacklistedRefreshTokens.contains(refreshToken)) {
                sessionManager.clearSessionAndLogout("Refresh token blacklisted")
                return@withLock false
            }

            val response = runCatching {
                refreshAuthApi.refreshToken(TokenRefreshRequest(refreshToken))
            }.getOrNull()

            if (response?.isSuccessful == true) {
                val body = response.body() ?: return@withLock false
                tokenManager.saveTokens(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    tokenType = body.tokenType,
                    expiresIn = body.expiresIn,
                    userId = body.user.id
                )
                return@withLock true
            }

            val code = response?.code()
            if (code == 401 || code == 403) {
                blacklistedRefreshTokens.add(refreshToken)
            }
            sessionManager.clearSessionAndLogout("Refresh failed: ${code ?: "network"}")
            false
        }
    }
}
