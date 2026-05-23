package com.example.duralapapp.data.local

import com.example.duralapapp.data.model.AuthEvent
import com.example.duralapapp.data.network.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    suspend fun clearSessionAndLogout(reason: String) {
        tokenManager.clearSession()
        AuthEventBus.emit(AuthEvent.Logout(reason))
    }
}
