package com.example.duralapapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Standard error response from the backend
 */
@JsonClass(generateAdapter = true)
data class ErrorResponse(
    @Json(name = "timestamp")
    val timestamp: String? = null,
    
    @Json(name = "status")
    val status: Int? = null,
    
    @Json(name = "error")
    val error: String? = null,
    
    @Json(name = "message")
    val message: String? = null,
    
    @Json(name = "path")
    val path: String? = null
) {
    companion object {
        fun parse(errorBody: String?): ErrorResponse? {
            if (errorBody.isNullOrBlank()) return null
            return try {
                val moshi = Moshi.Builder()
                    .addLast(KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(ErrorResponse::class.java)
                adapter.fromJson(errorBody)
            } catch (e: Exception) {
                null
            }
        }
    }
}
