package com.example.duralapapp.data.network

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val networkMonitor: NetworkMonitor,
    private val tokenRefreshCoordinator: TokenRefreshCoordinator,
    private val tokenWarmer: TokenWarmer
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!networkMonitor.isConnected()) {
            throw OfflineModeException()
        }

        val original = chain.request()
        if (isAuthRoute(original.url.encodedPath)) {
            return chain.proceed(original)
        }

        val accessToken = runBlocking {
            if (tokenManager.isAccessTokenExpired()) {
                val refreshed = tokenRefreshCoordinator.refreshIfNeeded(forceRefresh = true)
                if (!refreshed) {
                    throw SessionExpiredException()
                }
            } else {
                tokenWarmer.enqueuePrewarm()
            }
            tokenManager.accessToken.firstOrNull()
        }

        if (accessToken.isNullOrBlank()) {
            throw SessionExpiredException()
        }

        val request = original.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = chain.proceed(request)
        if (response.isSuccessful) {
            tokenWarmer.enqueuePrewarm()
        }
        return response
    }

    private fun isAuthRoute(path: String): Boolean {
        return path.endsWith("/api/auth/login") ||
            path.endsWith("/api/auth/register") ||
            path.endsWith("/api/auth/refresh")
    }
}
