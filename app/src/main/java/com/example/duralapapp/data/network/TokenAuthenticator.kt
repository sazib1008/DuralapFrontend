package com.example.duralapapp.data.network

import com.example.duralapapp.data.local.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRefreshCoordinator: TokenRefreshCoordinator,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            runBlocking { sessionManager.clearSessionAndLogout("401 retry limit reached") }
            return null
        }

        val refreshed = runBlocking { tokenRefreshCoordinator.refreshIfNeeded(forceRefresh = true) }
        if (!refreshed) return null

        val token = runBlocking { tokenManager.accessToken.firstOrNull() } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
