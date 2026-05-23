package com.example.duralapapp.data.api

import com.example.duralapapp.data.local.OfflineUiBus
import com.example.duralapapp.data.network.OfflineModeException

suspend fun <T> safeApiCall(
    apiCall: suspend () -> retrofit2.Response<T>
): Result<T> {
    return try {
        val response = apiCall()

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Empty response body"))
            }
        } else {
            Result.failure(
                Exception(response.errorBody()?.string() ?: response.message())
            )
        }

    } catch (e: Exception) {
        if (e is OfflineModeException) {
            OfflineUiBus.show()
        }
        Result.failure(e)
    }
}