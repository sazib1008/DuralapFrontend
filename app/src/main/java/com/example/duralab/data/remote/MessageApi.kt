package com.example.duralab.data.remote

import com.example.duralab.data.model.MessageCreateRequest
import com.example.duralab.data.model.MessageResponse
import com.example.duralab.data.model.MessageType
import retrofit2.Response
import retrofit2.http.*

interface MessageApi {
    @POST("api/messages")
    suspend fun sendMessage(@Body request: MessageCreateRequest): Response<MessageResponse>

    @GET("api/messages/conversation/{conversationId}")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<List<MessageResponse>>

    @GET("api/messages/conversation/{conversationId}/all")
    suspend fun getAllMessages(@Path("conversationId") conversationId: String): Response<List<MessageResponse>>

    @GET("api/messages/{id}")
    suspend fun getMessageById(@Path("id") id: String): Response<MessageResponse>

    @PATCH("api/messages/{id}/read")
    suspend fun markMessageAsRead(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): Response<MessageResponse>

    @PATCH("api/messages/conversation/{conversationId}/mark-all-read")
    suspend fun markAllMessagesAsRead(
        @Path("conversationId") conversationId: String,
        @Query("userId") userId: String
    ): Response<Unit>

    @GET("api/messages/conversation/{conversationId}/unread-count")
    suspend fun getUnreadMessagesCount(
        @Path("conversationId") conversationId: String,
        @Query("userId") userId: String
    ): Response<Long>

    @GET("api/messages/conversation/{conversationId}/unread")
    suspend fun getUnreadMessages(
        @Path("conversationId") conversationId: String,
        @Query("userId") userId: String
    ): Response<List<MessageResponse>>

    @GET("api/messages/conversation/{conversationId}/last")
    suspend fun getLastMessage(@Path("conversationId") conversationId: String): Response<MessageResponse>

    @DELETE("api/messages/{id}")
    suspend fun deleteMessage(
        @Path("id") id: String,
        @Query("userId") userId: String
    ): Response<Boolean>

    @GET("api/messages/conversation/{conversationId}/type/{messageType}")
    suspend fun getMessagesByType(
        @Path("conversationId") conversationId: String,
        @Path("messageType") messageType: MessageType
    ): Response<List<MessageResponse>>

    @GET("api/messages/conversation/{conversationId}/media")
    suspend fun getMediaMessages(@Path("conversationId") conversationId: String): Response<List<MessageResponse>>
}
