package com.example.duralapapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

enum class ConversationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    BLOCKED
}

@JsonClass(generateAdapter = true)
data class ConversationCreateRequest(
    @Json(name = "participantIds")
    val participantIds: Set<String>
)

@JsonClass(generateAdapter = true)
data class ConversationResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "participantIds")
    val participantIds: Set<String>,
    @Json(name = "status")
    val status: ConversationStatus = ConversationStatus.ACCEPTED,
    @Json(name = "createdAt")
    val createdAt: Instant,
    @Json(name = "lastMessage")
    val lastMessage: MessageResponse? = null,
    @Json(name = "unreadCount")
    val unreadCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class GetOrCreateConversationRequest(
    @Json(name = "user1Id")
    val user1Id: String,
    @Json(name = "user2Id")
    val user2Id: String
)

@JsonClass(generateAdapter = true)
data class StartConversationRequest(
    @Json(name = "targetUserId")
    val targetUserId: String,
    @Json(name = "initialMessage")
    val initialMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class ConversationRequestResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "senderId")
    val senderId: String,
    @Json(name = "senderUsername")
    val senderUsername: String? = null,
    @Json(name = "senderFullName")
    val senderFullName: String? = null,
    @Json(name = "senderProfileImageUrl")
    val senderProfileImageUrl: String? = null,
    @Json(name = "recipientId")
    val recipientId: String,
    @Json(name = "conversationId")
    val conversationId: String,
    @Json(name = "status")
    val status: ConversationStatus,
    @Json(name = "initialMessage")
    val initialMessage: String? = null,
    @Json(name = "requestedAt")
    val requestedAt: Instant,
    @Json(name = "respondedAt")
    val respondedAt: Instant? = null
)

@JsonClass(generateAdapter = true)
data class ConversationActionRequest(
    @Json(name = "conversationRequestId")
    val conversationRequestId: String
)

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    SYSTEM
}

enum class MessageStatus {
    SENT,
    DELIVERED,
    READ,
    FAILED
}
