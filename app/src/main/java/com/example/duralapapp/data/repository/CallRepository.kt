package com.example.duralapapp.data.repository

import com.example.duralapapp.data.api.CallApi
import com.example.duralapapp.data.api.safeApiCall
import com.example.duralapapp.data.model.CallHistoryItemResponse
import com.example.duralapapp.data.model.CallInitiateRequest
import com.example.duralapapp.data.model.CallResponse
import com.example.duralapapp.data.model.CallType
import com.example.duralapapp.data.model.WebRTCSignal
import com.example.duralapapp.data.websocket.StompWebSocketClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

interface CallRepository {
    suspend fun initiateCall(conversationId: String, callerId: String, calleeId: String, callType: CallType): Result<CallResponse>
    suspend fun acceptCall(callId: String, userId: String): Result<CallResponse>
    suspend fun rejectCall(callId: String, userId: String): Result<CallResponse>
    suspend fun endCall(callId: String, userId: String): Result<CallResponse>
    suspend fun getCallHistory(userId: String): Result<List<CallHistoryItemResponse>>
    
    fun observeUserSignaling(userId: String): Flow<WebRTCSignal>
    fun sendSignalingEvent(targetUserId: String, signal: WebRTCSignal)
}

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val callApi: CallApi,
    private val stompClient: StompWebSocketClient,
    private val moshi: Moshi
) : CallRepository {

    private val rtcAdapter by lazy { moshi.adapter(WebRTCSignal::class.java) }

    override suspend fun initiateCall(
        conversationId: String,
        callerId: String,
        calleeId: String,
        callType: CallType
    ): Result<CallResponse> {
        return safeApiCall {
            callApi.initiateCall(
                CallInitiateRequest(
                    conversationId = conversationId,
                    callerId = callerId,
                    calleeId = calleeId,
                    callType = callType
                )
            )
        }
    }

    override suspend fun acceptCall(callId: String, userId: String): Result<CallResponse> {
        return safeApiCall { callApi.acceptCall(callId, userId) }
    }

    override suspend fun rejectCall(callId: String, userId: String): Result<CallResponse> {
        return safeApiCall { callApi.rejectCall(callId, userId) }
    }

    override suspend fun endCall(callId: String, userId: String): Result<CallResponse> {
        return safeApiCall { callApi.endCall(callId, userId) }
    }

    override suspend fun getCallHistory(userId: String): Result<List<CallHistoryItemResponse>> {
        return safeApiCall { callApi.getCallHistoryList(userId) }
    }

    override fun observeUserSignaling(userId: String): Flow<WebRTCSignal> {
        val topic = "/topic/user/$userId/signaling"
        stompClient.subscribe(topic)

        return stompClient.messages
            .filter { it.headers["destination"] == topic }
            .mapNotNull { msg ->
                try {
                    rtcAdapter.fromJson(msg.payload)
                } catch (e: Exception) {
                    null
                }
            }
    }

    override fun sendSignalingEvent(targetUserId: String, signal: WebRTCSignal) {
        val destination = "/topic/user/$targetUserId/signaling"
        val json = rtcAdapter.toJson(signal)
        stompClient.send(destination, json)
    }
}
