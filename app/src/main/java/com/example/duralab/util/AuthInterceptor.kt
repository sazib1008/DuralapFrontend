package com.example.duralab.util

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        
        // Exclude endpoints that do not require authentication (e.g., login, register, refresh)
        val path = chain.request().url.encodedPath
        if (path.contains("login") || path.contains("register") || path.contains("refresh")) {
            return chain.proceed(chain.request())
        }

        val token = tokenManager.getToken()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
