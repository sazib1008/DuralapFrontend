package com.example.duralab.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConversationCreateRequest(
    @Json(name = "participantIds") val participantIds: Set<String>
)

@JsonClass(generateAdapter = true)
data class ConversationResponse(
    @Json(name = "id") val id: String,
    @Json(name = "participantIds") val participantIds: Set<String>,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "lastMessage") val lastMessage: MessageResponse? = null,
    @Json(name = "unreadCount") val unreadCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class GetOrCreateConversationRequest(
    @Json(name = "user1Id") val user1Id: String,
    @Json(name = "user2Id") val user2Id: String
)
