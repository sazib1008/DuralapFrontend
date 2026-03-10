package com.example.duralab.data.remote

import com.example.duralab.data.model.ConversationCreateRequest
import com.example.duralab.data.model.ConversationResponse
import com.example.duralab.data.model.GetOrCreateConversationRequest
import com.example.duralab.data.model.UserDto
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {
    @POST("api/conversations")
    suspend fun createConversation(@Body request: ConversationCreateRequest): Response<ConversationResponse>

    @POST("api/conversations/get-or-create")
    suspend fun getOrCreateConversation(@Body request: GetOrCreateConversationRequest): Response<ConversationResponse>

    @GET("api/conversations/{id}")
    suspend fun getConversationById(@Path("id") id: String): Response<ConversationResponse>

    @GET("api/conversations/user/{userId}")
    suspend fun getConversationsForUser(@Path("userId") userId: String): Response<List<ConversationResponse>>

    @DELETE("api/conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: String): Response<Unit>

    @POST("api/conversations/{conversationId}/participants/{userId}")
    suspend fun addParticipant(
        @Path("conversationId") conversationId: String,
        @Path("userId") userId: String
    ): Response<ConversationResponse>

    @DELETE("api/conversations/{conversationId}/participants/{userId}")
    suspend fun removeParticipant(
        @Path("conversationId") conversationId: String,
        @Path("userId") userId: String
    ): Response<ConversationResponse>

    @GET("api/conversations/{conversationId}/participants")
    suspend fun getConversationParticipants(@Path("conversationId") conversationId: String): Response<List<UserDto>>
}
