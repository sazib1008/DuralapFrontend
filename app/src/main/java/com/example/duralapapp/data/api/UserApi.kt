package com.example.duralapapp.data.api

import com.example.duralapapp.data.model.UserResponse
import com.example.duralapapp.data.model.UserStatus
import com.example.duralapapp.data.model.UserUpdateRequest
import retrofit2.Response
import retrofit2.http.*

interface UserApi {

    @GET("api/auth/profile")
    suspend fun getCurrentUserProfile(): Response<UserResponse>

    @GET("api/users/{id}")
    suspend fun getUserById(
        @Path("id") id: String
    ): Response<UserResponse>

    @GET("api/users/username/{username}")
    suspend fun getUserByUsername(
        @Path("username") username: String
    ): Response<UserResponse>

    @GET("api/users/email/{email}")
    suspend fun getUserByEmail(
        @Path("email") email: String
    ): Response<UserResponse>

    @PUT("api/users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: UserUpdateRequest
    ): Response<UserResponse>

    @DELETE("api/users/{id}")
    suspend fun deleteUser(
        @Path("id") id: String
    ): Response<Unit>

    @PATCH("api/users/{id}/status")
    suspend fun updateUserStatus(
        @Path("id") id: String,
        @Query("status") status: UserStatus
    ): Response<UserResponse>

    @GET("api/users/check-username/{username}")
    suspend fun checkUsername(
        @Path("username") username: String
    ): Response<Map<String, Boolean>>

    @GET("api/users/check-email/{email}")
    suspend fun checkEmail(
        @Path("email") email: String
    ): Response<Map<String, Boolean>>
}

