package com.example.duralab.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String,
    @Json(name = "expiresIn") val expiresIn: Long,
    @Json(name = "user") val user: com.example.duralab.data.model.UserResponse
)

@JsonClass(generateAdapter = true)
data class TokenRefreshRequest(
    @Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "profilePicture") val profilePicture: String?,
    @Json(name = "status") val status: String? = "online"
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "usernameOrEmail") val usernameOrEmail: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)
