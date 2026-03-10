package com.example.duralab.util

import android.content.Context
import android.content.Intent
import com.example.duralab.data.model.TokenRefreshRequest
import com.example.duralab.data.remote.AuthApi
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: Lazy<AuthApi>,
    @ApplicationContext private val context: Context
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val currentToken = tokenManager.getToken()
        
        // Ensure we don't end up in an infinite loop if the refresh API itself returns 401
        if (response.request.url.encodedPath.contains("auth/refresh")) {
            return null
        }

        synchronized(this) {
            val newToken = tokenManager.getToken()
            // If another thread already successfully refreshed the token while we were waiting
            if (currentToken != newToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshToken() ?: return handleLogout()

            return runBlocking {
                try {
                    // Use Lazy<AuthApi> to avoid circular dependency injection issues
                    val refreshResponse = authApi.get().refreshToken(TokenRefreshRequest(refreshToken))

                    if (refreshResponse.isSuccessful) {
                        refreshResponse.body()?.let { authResponse ->
                            tokenManager.saveToken(authResponse.accessToken)
                            tokenManager.saveRefreshToken(authResponse.refreshToken)
                            
                            return@runBlocking response.request.newBuilder()
                                .header("Authorization", "Bearer ${authResponse.accessToken}")
                                .build()
                        }
                    } 
                    
                    // Refresh token is expired or invalid
                    return@runBlocking handleLogout()
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@runBlocking handleLogout()
                }
            }
        }
    }

    private fun handleLogout(): Request? {
        tokenManager.clear()
        // Send user to login screen via Intent
        val intent = Intent(context, Class.forName("com.example.duralab.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("FORCE_LOGOUT", true)
        }
        context.startActivity(intent)
        return null
    }
}
