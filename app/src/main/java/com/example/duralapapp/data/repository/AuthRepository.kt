package com.example.duralapapp.data.repository

import com.example.duralapapp.data.api.AuthApi
import com.example.duralapapp.data.api.safeApiCall
import com.example.duralapapp.data.model.*
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository ইন্টারফেস যা ডোমেইন লেয়ারের জন্য ডেটা মেথডগুলো ডিফাইন করে।
 */
interface AuthRepository {
    suspend fun register(request: UserCreateRequest): Result<UserResponse>
    suspend fun login(request: LoginRequest): Result<AuthResponse>
    suspend fun refreshToken(request: TokenRefreshRequest): Result<AuthResponse>
    suspend fun logout(token: String): Result<Unit>
    suspend fun getCurrentUser(): Result<UserResponse>
}
/**
 * AuthRepository এর ইমপ্লিমেন্টেশন।
 * এখানে Hilt বা Dagger এর মাধ্যমে AuthApi ইনজেক্ট করা হয়েছে।
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi
) : AuthRepository {

    override suspend fun register(
        request: UserCreateRequest
    ): Result<UserResponse> {
        return safeApiCall {
            authApi.register(request)
        }
    }

    override suspend fun login(
        request: LoginRequest
    ): Result<AuthResponse> {
        return safeApiCall {
            authApi.login(request)
        }
    }

    override suspend fun refreshToken(
        request: TokenRefreshRequest
    ): Result<AuthResponse> {
        return safeApiCall {
            authApi.refreshToken(request)
        }
    }

    override suspend fun logout(
        token: String
    ): Result<Unit> {

        val formattedToken =
            if (token.startsWith("Bearer ")) token
            else "Bearer $token"

        return safeApiCall {
            authApi.logout(formattedToken)
        }
    }

    override suspend fun getCurrentUser(): Result<UserResponse> {
        return safeApiCall {
            authApi.getCurrentUser()
        }
    }
}