package com.example.duralab.data.remote

import com.example.duralab.data.model.UserResponse
import com.example.duralab.data.model.UserUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<UserResponse>

    @PUT("api/users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: UserUpdateRequest
    ): Response<UserResponse>
}
