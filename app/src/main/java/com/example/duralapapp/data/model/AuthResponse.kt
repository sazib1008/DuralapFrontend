package com.example.duralapapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

enum class Role {
    USER,
    ADMIN
}

enum class UserStatus {
    ONLINE,
    OFFLINE,
    AWAY,
    BUSY
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "usernameOrEmail")
    val usernameOrEmail: String,
    @Json(name = "password")
    val password: String
)

@JsonClass(generateAdapter = true)
data class TokenRefreshRequest(
    @Json(name = "refreshToken")
    val refreshToken: String
)



@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "accessToken")
    val accessToken: String,
    @Json(name = "refreshToken")
    val refreshToken: String,
    @Json(name = "tokenType")
    val tokenType: String = "Bearer",
    @Json(name = "expiresIn")
    val expiresIn: Long,
    @Json(name = "user")
    val user: UserResponse,
    @Json(name = "userConversations")
    val userConversations: UserConversationsDto? = null
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "username")
    val username: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "fullName")
    val fullName: String?,
    @Json(name = "bio")
    val bio: String?,
    @Json(name = "profileImageUrl")
    val profileImageUrl: String?,
    @Json(name = "phoneNumber")
    val phoneNumber: String?,
    @Json(name = "isVerified")
    val isVerified: Boolean,
    @Json(name = "status")
    val status: UserStatus,
    @Json(name = "lastSeen")
    val lastSeen: Instant?,
    @Json(name = "isInCall")
    val isInCall: Boolean,
    @Json(name = "currentCallId")
    val currentCallId: String?,
    @Json(name = "roles")
    val roles: Set<Role>,
    @Json(name = "createdAt")
    val createdAt: Instant,
    @Json(name = "updatedAt")
    val updatedAt: Instant
)

@JsonClass(generateAdapter = true)
data class PublicUserProfile(
    @Json(name = "id")
    val id: String,
    @Json(name = "username")
    val username: String,
    @Json(name = "fullName")
    val fullName: String?,
    @Json(name = "bio")
    val bio: String?,
    @Json(name = "profileImageUrl")
    val profileImageUrl: String?,
    @Json(name = "status")
    val status: UserStatus,
    @Json(name = "isVerified")
    val isVerified: Boolean,
    @Json(name = "lastSeen")
    val lastSeen: Instant?
)

@JsonClass(generateAdapter = true)
data class UserConversationsDto(
    @Json(name = "userId")
    val userId: String,
    @Json(name = "conversationIds")
    val conversationIds: Set<String>
)

@JsonClass(generateAdapter = true)
data class UserCreateRequest(
    @Json(name = "username")
    val username: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "password")
    val password: String,
    @Json(name = "fullName")
    val fullName: String? = null,
    @Json(name = "bio")
    val bio: String? = null,
    @Json(name = "phoneNumber")
    val phoneNumber: String? = null,
    @Json(name = "roles")
    val roles: Set<Role> = setOf(Role.USER)
)

@JsonClass(generateAdapter = true)
data class UserUpdateRequest(
    @Json(name = "fullName")
    val fullName: String? = null,
    @Json(name = "bio")
    val bio: String? = null,
    @Json(name = "profileImageUrl")
    val profileImageUrl: String? = null,
    @Json(name = "phoneNumber")
    val phoneNumber: String? = null,
    @Json(name = "status")
    val status: UserStatus? = null,
    @Json(name = "isVerified")
    val isVerified: Boolean? = null,
    @Json(name = "roles")
    val roles: Set<Role>? = null
)

@JsonClass(generateAdapter = true)
data class UserPresenceEvent(
    @Json(name = "userId")
    val userId: String,
    @Json(name = "status")
    val status: UserStatus,
    @Json(name = "lastSeen")
    val lastSeen: Instant? = null,
    @Json(name = "sessionCount")
    val sessionCount: Int = 0,
    @Json(name = "timestamp")
    val timestamp: Instant? = null
)

@JsonClass(generateAdapter = true)
data class BatchPresenceRequest(
    @Json(name = "userIds")
    val userIds: List<String>
)
