package com.example.duralab.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class MessageType {
    TEXT,
    IMAGE,
    DOCUMENT,
    AUDIO,
    VIDEO
}

@JsonClass(generateAdapter = true)
data class UserInfo(
    @Json(name = "id") val id: String,
    @Json(name = "username") val username: String,
    @Json(name = "fullName") val fullName: String?,
    @Json(name = "profileImageUrl") val profileImageUrl: String?
)

@JsonClass(generateAdapter = true)
data class MessageCreateRequest(
    @Json(name = "conversationId") val conversationId: String,
    @Json(name = "senderId") val senderId: String,
    @Json(name = "content") val content: String,
    @Json(name = "messageType") val messageType: MessageType = MessageType.TEXT,
    @Json(name = "mediaUrl") val mediaUrl: String? = null,
    @Json(name = "mediaType") val mediaType: String? = null,
    @Json(name = "fileName") val fileName: String? = null,
    @Json(name = "fileSize") val fileSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    @Json(name = "id") val id: String,
    @Json(name = "conversationId") val conversationId: String,
    @Json(name = "senderId") val senderId: String,
    @Json(name = "content") val content: String,
    @Json(name = "messageType") val messageType: MessageType,
    @Json(name = "mediaUrl") val mediaUrl: String?,
    @Json(name = "mediaType") val mediaType: String?,
    @Json(name = "fileName") val fileName: String?,
    @Json(name = "fileSize") val fileSize: Long?,
    @Json(name = "isRead") val isRead: Boolean,
    @Json(name = "readAt") val readAt: String?,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String,
    @Json(name = "senderInfo") val senderInfo: UserInfo? = null
)

@JsonClass(generateAdapter = true)
data class MessageReadRequest(
    @Json(name = "messageId") val messageId: String,
    @Json(name = "userId") val userId: String
)
