package com.example.duralapapp.data.api

import com.example.duralapapp.data.model.ConversationActionRequest
import com.example.duralapapp.data.model.ConversationRequestResponse
import com.example.duralapapp.data.model.ConversationResponse
import com.example.duralapapp.data.model.StartConversationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ConversationRequestApi {

    @POST("api/conversations/start-with")
    suspend fun startConversationWithUser(
        @Body request: StartConversationRequest
    ): Response<ConversationRequestResponse>

    @GET("api/conversation-requests/pending")
    suspend fun getPendingRequests(): Response<List<ConversationRequestResponse>>

    @GET("api/conversation-requests/pending/count")
    suspend fun getPendingRequestCount(): Response<Map<String, Int>>

    @POST("api/conversation-requests/accept")
    suspend fun acceptRequest(
        @Body request: ConversationActionRequest
    ): Response<ConversationResponse>

    @POST("api/conversation-requests/reject")
    suspend fun rejectRequest(
        @Body request: ConversationActionRequest
    ): Response<ConversationRequestResponse>

    @POST("api/conversation-requests/cancel")
    suspend fun cancelRequest(
        @Body request: ConversationActionRequest
    ): Response<Unit>
}
