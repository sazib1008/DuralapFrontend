package com.example.duralapapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallStatus {
    RINGING,
    CONNECTED,
    MISSED,
    REJECTED,
    ENDED,
    FAILED
}

enum class SignalType {
    OFFER,
    ANSWER,
    ICE_CANDIDATE
}

@JsonClass(generateAdapter = true)
data class CallInitiateRequest(
    @Json(name = "conversationId")
    val conversationId: String,
    @Json(name = "callerId")
    val callerId: String,
    @Json(name = "calleeId")
    val calleeId: String,
    @Json(name = "callType")
    val callType: CallType
)

@JsonClass(generateAdapter = true)
data class CallResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "conversationId")
    val conversationId: String,
    @Json(name = "callerId")
    val callerId: String,
    @Json(name = "calleeId")
    val calleeId: String,
    @Json(name = "callType")
    val callType: CallType,
    @Json(name = "status")
    val status: CallStatus,
    @Json(name = "startTime")
    val startTime: Instant?,
    @Json(name = "endTime")
    val endTime: Instant?,
    @Json(name = "duration")
    val duration: Long?,
    @Json(name = "createdAt")
    val createdAt: Instant,
    @Json(name = "updatedAt")
    val updatedAt: Instant
)

@JsonClass(generateAdapter = true)
data class CallActionRequest(
    @Json(name = "callId")
    val callId: String,
    @Json(name = "userId")
    val userId: String,
    @Json(name = "status")
    val status: CallStatus
)

@JsonClass(generateAdapter = true)
data class WebRTCSignal(
    @Json(name = "callId")
    val callId: String,
    @Json(name = "senderId")
    val senderId: String,
    @Json(name = "targetId")
    val targetId: String? = null,
    @Json(name = "type")
    val type: SignalType,
    @Json(name = "data")
    val data: String? = null,
    @Json(name = "sdpData")
    val sdpData: String? = null,
    @Json(name = "iceCandidate")
    val iceCandidate: String? = null
)


@JsonClass(generateAdapter = true)
data class CallStatusUpdate(
    @Json(name = "callId")
    val callId: String,
    @Json(name = "status")
    val status: CallStatus,
    @Json(name = "userId")
    val userId: String,
    @Json(name = "timestamp")
    val timestamp: Instant = Instant.now()
)

@JsonClass(generateAdapter = true)
data class CallHistoryItemResponse(
    @Json(name = "id")
    val id: String,
    @Json(name = "conversationId")
    val conversationId: String,
    @Json(name = "callerId")
    val callerId: String,
    @Json(name = "calleeId")
    val calleeId: String,
    @Json(name = "callType")
    val callType: CallType,
    @Json(name = "status")
    val status: CallStatus,
    @Json(name = "startTime")
    val startTime: Instant?,
    @Json(name = "endTime")
    val endTime: Instant?,
    @Json(name = "duration")
    val duration: Long?,
    @Json(name = "createdAt")
    val createdAt: Instant,
    @Json(name = "updatedAt")
    val updatedAt: Instant,
    @Json(name = "otherUser")
    val otherUser: PublicUserProfile?,
    @Json(name = "isIncoming")
    val isIncoming: Boolean
)
