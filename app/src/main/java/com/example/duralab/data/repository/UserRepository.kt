package com.example.duralab.data.repository

import com.example.duralab.data.local.dao.UserDao
import com.example.duralab.data.local.entity.UserEntity
import com.example.duralab.data.model.UserResponse
import com.example.duralab.data.model.UserUpdateRequest
import com.example.duralab.data.remote.AuthApi
import com.example.duralab.data.remote.UserApi
import com.example.duralab.util.TokenManager
import com.example.duralab.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val tokenManager: TokenManager
) {
    fun getOnlineUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun syncUsers() {
        // Dummy data for now
        userDao.insertUser(UserEntity("1", "John Doe", "john@example.com", null, "online", System.currentTimeMillis()))
        userDao.insertUser(UserEntity("2", "Jane Smith", "jane@example.com", null, "away", System.currentTimeMillis()))
    }

    fun getCurrentUserId(): String? = tokenManager.getUserId()
    fun getCurrentUserEmail(): String? = tokenManager.getUserEmail()

    suspend fun getUserProfile(userId: String): Flow<UiState<UserResponse>> = flow {
        emit(UiState.Loading)
        try {
            val response = userApi.getUserById(userId)
            if (response.isSuccessful && response.body() != null) {
                emit(UiState.Success(response.body()!!))
            } else {
                emit(UiState.Error(response.message() ?: "Failed to fetch profile"))
            }
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "An unknown error occurred"))
        }
    }

    suspend fun updateUserProfile(userId: String, request: UserUpdateRequest): Flow<UiState<UserResponse>> = flow {
        emit(UiState.Loading)
        try {
            val response = userApi.updateUser(userId, request)
            if (response.isSuccessful && response.body() != null) {
                val updatedUser = response.body()!!
                tokenManager.saveUser(updatedUser.id, updatedUser.username, updatedUser.email)
                emit(UiState.Success(updatedUser))
            } else {
                emit(UiState.Error(response.message() ?: "Failed to update profile"))
            }
        } catch (e: Exception) {
            emit(UiState.Error(e.message ?: "An unknown error occurred"))
        }
    }
}
