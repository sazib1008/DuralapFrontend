package com.example.duralab.data.repository

import com.example.duralab.data.model.CallHistoryItemResponse
import com.example.duralab.data.remote.CallApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val callApi: CallApi
) {
    suspend fun getCallHistoryList(userId: String, limit: Int = 50): Result<List<CallHistoryItemResponse>> {
        return try {
            val response = callApi.getCallHistoryList(userId, limit)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
