package com.example.duralab.data.repository

import com.example.duralab.data.local.dao.UserDao
import com.example.duralab.data.local.entity.UserEntity
import com.example.duralab.data.remote.AuthApi
import com.example.duralab.util.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val authApi: AuthApi // Or a dedicated UserApi
) {
    fun getOnlineUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun syncUsers() {
        // In a real app, fetch from network and insert to DAO
        // val response = authApi.getUsers() 
        // if (response.isSuccessful) { response.body()?.forEach { userDao.insertUser(it.toEntity()) } }
        
        // Dummy data for now
        userDao.insertUser(UserEntity("1", "John Doe", "john@example.com", null, "online", System.currentTimeMillis()))
        userDao.insertUser(UserEntity("2", "Jane Smith", "jane@example.com", null, "away", System.currentTimeMillis()))
    }
}
