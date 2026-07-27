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
            } else if (response.code() == 204 || response.code() == 205) {
                @Suppress("UNCHECKED_CAST")
                Result.success(Unit as T)
            } else {
                @Suppress("UNCHECKED_CAST")
                runCatching { Unit as T }
                    .fold(
                        onSuccess = { Result.success(it) },
                        onFailure = { Result.failure(Exception("Empty response body")) }
                    )
            }
        } else {
            val rawError = response.errorBody()?.string()
            val parsedError = com.example.duralapapp.data.model.ErrorResponse.parse(rawError)
            val errorMessage = parsedError?.message?.takeIf { it.isNotBlank() }
                ?: parsedError?.error?.takeIf { it.isNotBlank() }
                ?: rawError
                ?: response.message()
            Result.failure(Exception(errorMessage))
        }

    } catch (e: Exception) {
        if (e is OfflineModeException) {
            OfflineUiBus.show()
        }
        Result.failure(e)
    }
}