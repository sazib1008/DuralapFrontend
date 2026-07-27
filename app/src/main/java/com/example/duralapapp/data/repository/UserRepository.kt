package com.example.duralapapp.data.repository

import com.example.duralapapp.data.api.UserApi
import com.example.duralapapp.data.api.safeApiCall
import com.example.duralapapp.data.model.UserResponse
import com.example.duralapapp.data.model.UserUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    suspend fun getCurrentUserProfile(): Result<UserResponse>
    suspend fun updateUserProfile(id: String, request: UserUpdateRequest): Result<UserResponse>
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi
) : UserRepository {

    override suspend fun getCurrentUserProfile(): Result<UserResponse> {
        return safeApiCall { userApi.getCurrentUserProfile() }
    }

    override suspend fun updateUserProfile(
        id: String,
        request: UserUpdateRequest
    ): Result<UserResponse> {
        return safeApiCall { userApi.updateUser(id, request) }
    }
}
