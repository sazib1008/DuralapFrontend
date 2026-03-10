package com.example.duralab.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallStatus {
    INITIATED,
    RINGING,
    ACCEPTED,
    REJECTED,
    MISSED,
    ENDED,
    ONGOING
}

enum class SignalType {
    OFFER,
    ANSWER,
    ICE_CANDIDATE
}

@JsonClass(generateAdapter = true)
data class CallInitiateRequest(
    @Json(name = "conversationId") val conversationId: String,
    @Json(name = "callerId") val callerId: String,
    @Json(name = "calleeId") val calleeId: String,
    @Json(name = "callType") val callType: CallType
)

@JsonClass(generateAdapter = true)
data class CallResponse(
    @Json(name = "id") val id: String,
    @Json(name = "conversationId") val conversationId: String,
    @Json(name = "callerId") val callerId: String,
    @Json(name = "calleeId") val calleeId: String,
    @Json(name = "callType") val callType: CallType,
    @Json(name = "status") val status: CallStatus,
    @Json(name = "startTime") val startTime: String?,
    @Json(name = "endTime") val endTime: String?,
    @Json(name = "duration") val duration: Long?,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class CallActionRequest(
    @Json(name = "callId") val callId: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "status") val status: CallStatus
)

@JsonClass(generateAdapter = true)
data class WebRTCSignal(
    @Json(name = "callId") val callId: String,
    @Json(name = "senderId") val senderId: String,
    @Json(name = "targetId") val targetId: String,
    @Json(name = "type") val type: SignalType,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class CallStatusUpdate(
    @Json(name = "callId") val callId: String,
    @Json(name = "status") val status: CallStatus,
    @Json(name = "userId") val userId: String,
    @Json(name = "timestamp") val timestamp: String? = null
)
