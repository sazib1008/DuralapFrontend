package com.example.duralab.data.remote

import com.example.duralab.data.model.AuthResponse
import com.example.duralab.data.model.LoginRequest
import com.example.duralab.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<com.example.duralab.data.model.UserResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: com.example.duralab.data.model.TokenRefreshRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Void>

    @GET("api/auth/profile")
    suspend fun profile(): Response<com.example.duralab.data.model.UserResponse>
}
