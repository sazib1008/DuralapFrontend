package com.example.duralab.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "profilePicture") val profilePicture: String? = null,
    @Json(name = "status") val status: String? = "ONLINE",
    @Json(name = "isEmailVerified") val isEmailVerified: Boolean = false,
    @Json(name = "isInCall") val isInCall: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UserUpdateRequest(
    @Json(name = "username") val username: String? = null,
    @Json(name = "profilePicture") val profilePicture: String? = null
)
