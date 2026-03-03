package com.example.duralab.data.remote

import com.example.duralab.data.model.AuthResponse
import com.example.duralab.data.model.LoginRequest
import com.example.duralab.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body refreshToken: String): Response<AuthResponse>
}
