package com.example.duralapapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallStatus {
    IDLE,
    INITIATED,
    CALLING,
    RINGING,
    ACCEPTED,
    CONNECTING,
    CONNECTED,
    ACTIVE,
    ENDED,
    REJECTED,
    BUSY,
    CANCELLED,
    TIMEOUT,
    MISSED,
    FAILED
}

enum class SignalType {
    CALL_INITIATE,
    CALL_RINGING,
    CALL_ACCEPT,
    CALL_REJECT,
    CALL_BUSY,
    CALL_CANCEL,
    CALL_END,
    CALL_TIMEOUT,
    WEBRTC_OFFER,
    WEBRTC_ANSWER,
    ICE_CANDIDATE,
    CALL_CONNECTED,
    CALL_FAILED,
    OFFER,
    ANSWER
}

@JsonClass(generateAdapter = true)
data class CallSignalingMessage(
    @Json(name = "callId")
    val callId: String,
    @Json(name = "conversationId")
    val conversationId: String? = null,
    @Json(name = "callerId")
    val callerId: String? = null,
    @Json(name = "calleeId")
    val calleeId: String? = null,
    @Json(name = "senderId")
    val senderId: String,
    @Json(name = "targetUserId")
    val targetUserId: String,
    @Json(name = "callType")
    val callType: CallType = CallType.AUDIO,
    @Json(name = "signalType")
    val signalType: SignalType,
    @Json(name = "callerName")
    val callerName: String? = null,
    @Json(name = "callerAvatar")
    val callerAvatar: String? = null,
    @Json(name = "sdp")
    val sdp: String? = null,
    @Json(name = "sdpData")
    val sdpData: String? = null,
    @Json(name = "iceCandidate")
    val iceCandidate: String? = null,
    @Json(name = "sdpMid")
    val sdpMid: String? = null,
    @Json(name = "sdpMLineIndex")
    val sdpMLineIndex: Int? = null,
    @Json(name = "reason")
    val reason: String? = null,
    @Json(name = "timestamp")
    val timestamp: Instant? = null
)

@JsonClass(generateAdapter = true)
data class IceServerConfig(
    @Json(name = "urls")
    val urls: List<String>,
    @Json(name = "username")
    val username: String? = null,
    @Json(name = "credential")
    val credential: String? = null
)

@JsonClass(generateAdapter = true)
data class CallIceServersResponse(
    @Json(name = "iceServers")
    val iceServers: List<IceServerConfig>
)

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
