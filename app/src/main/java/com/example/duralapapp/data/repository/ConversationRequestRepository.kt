package com.example.duralapapp.data.repository

import com.example.duralapapp.data.api.ConversationRequestApi
import com.example.duralapapp.data.api.safeApiCall
import com.example.duralapapp.data.model.ConversationActionRequest
import com.example.duralapapp.data.model.ConversationRequestResponse
import com.example.duralapapp.data.model.ConversationResponse
import com.example.duralapapp.data.model.StartConversationRequest
import javax.inject.Inject
import javax.inject.Singleton

interface ConversationRequestRepository {
    suspend fun startConversationWithUser(targetUserId: String, initialMessage: String? = null): Result<ConversationRequestResponse>
    suspend fun getPendingRequests(): Result<List<ConversationRequestResponse>>
    suspend fun getPendingRequestCount(): Result<Int>
    suspend fun acceptRequest(requestId: String): Result<ConversationResponse>
    suspend fun rejectRequest(requestId: String): Result<ConversationRequestResponse>
    suspend fun cancelRequest(requestId: String): Result<Unit>
}

@Singleton
class ConversationRequestRepositoryImpl @Inject constructor(
    private val api: ConversationRequestApi
) : ConversationRequestRepository {

    override suspend fun startConversationWithUser(
        targetUserId: String,
        initialMessage: String?
    ): Result<ConversationRequestResponse> {
        return safeApiCall {
            api.startConversationWithUser(StartConversationRequest(targetUserId, initialMessage))
        }
    }

    override suspend fun getPendingRequests(): Result<List<ConversationRequestResponse>> {
        return safeApiCall {
            api.getPendingRequests()
        }
    }

    override suspend fun getPendingRequestCount(): Result<Int> {
        return safeApiCall {
            api.getPendingRequestCount()
        }.map { map ->
            map["count"] ?: 0
        }
    }

    override suspend fun acceptRequest(requestId: String): Result<ConversationResponse> {
        return safeApiCall {
            api.acceptRequest(ConversationActionRequest(requestId))
        }
    }

    override suspend fun rejectRequest(requestId: String): Result<ConversationRequestResponse> {
        return safeApiCall {
            api.rejectRequest(ConversationActionRequest(requestId))
        }
    }

    override suspend fun cancelRequest(requestId: String): Result<Unit> {
        return safeApiCall {
            api.cancelRequest(ConversationActionRequest(requestId))
        }
    }
}
