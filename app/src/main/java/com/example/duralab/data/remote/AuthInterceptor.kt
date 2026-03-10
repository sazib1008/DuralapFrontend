package com.example.duralab.data.remote

import com.example.duralab.util.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: Provider<AuthApi> // Use Provider to avoid circular dependency
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getToken()

        val requestBuilder = originalRequest.newBuilder()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            // Handle 401 Unauthorized - Token might be expired
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                // Synchronously call refresh token
                val tokenResponse = runBlocking { authApi.get().refreshToken(com.example.duralab.data.model.TokenRefreshRequest(refreshToken)) }
                if (tokenResponse.isSuccessful && tokenResponse.body() != null) {
                    val newToken = tokenResponse.body()!!.accessToken
                    tokenManager.saveToken(newToken)
                    
                    // Retry original request with new token
                    val newRequest = originalRequest.newBuilder()
                        .addHeader("Authorization", "Bearer $newToken")
                        .build()
                    response.close()
                    return chain.proceed(newRequest)
                }
            }
        }

        return response
    }
}
