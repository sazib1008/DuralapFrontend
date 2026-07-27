package com.example.duralapapp.data.repository

import com.example.duralapapp.data.api.ConversationApi
import com.example.duralapapp.data.api.MessageApi
import com.example.duralapapp.data.api.safeApiCall
import com.example.duralapapp.data.model.*
import com.example.duralapapp.data.websocket.StompWebSocketClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    suspend fun getMyConversations(): Result<List<ConversationResponse>>
    suspend fun getConversationById(id: String): Result<ConversationResponse>
    suspend fun getMessages(conversationId: String, page: Int = 0, size: Int = 30): Result<List<MessageResponse>>
    suspend fun sendMessage(conversationId: String, senderId: String, content: String): Result<MessageResponse>
    suspend fun markAllAsRead(conversationId: String): Result<Unit>
    
    fun observeConversationMessages(conversationId: String): Flow<MessageResponse>
    fun observeUserMessages(userId: String): Flow<MessageResponse>
    fun connectWebSocket(token: String)
    fun disconnectWebSocket()
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationApi: ConversationApi,
    private val messageApi: MessageApi,
    private val stompClient: StompWebSocketClient,
    private val moshi: Moshi
) : ChatRepository {

    private val messageAdapter by lazy { moshi.adapter(MessageResponse::class.java) }

    override suspend fun getMyConversations(): Result<List<ConversationResponse>> {
        return safeApiCall { conversationApi.getMyConversations() }
    }

    override suspend fun getConversationById(id: String): Result<ConversationResponse> {
        return safeApiCall { conversationApi.getConversationById(id) }
    }

    override suspend fun getMessages(conversationId: String, page: Int, size: Int): Result<List<MessageResponse>> {
        return safeApiCall { messageApi.getMessages(conversationId, page, size) }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String
    ): Result<MessageResponse> {
        return safeApiCall {
            messageApi.sendMessage(
                MessageCreateRequest(
                    conversationId = conversationId,
                    senderId = senderId,
                    content = content
                )
            )
        }
    }

    override suspend fun markAllAsRead(conversationId: String): Result<Unit> {
        return safeApiCall { messageApi.markAllMessagesAsRead(conversationId) }
    }

    override fun observeConversationMessages(conversationId: String): Flow<MessageResponse> {
        val topic = "/topic/conversation/$conversationId"
        stompClient.subscribe(topic)

        return stompClient.messages
            .filter { it.headers["destination"] == topic }
            .mapNotNull { msg ->
                try {
                    messageAdapter.fromJson(msg.payload)
                } catch (e: Exception) {
                    null
                }
            }
    }

    override fun observeUserMessages(userId: String): Flow<MessageResponse> {
        val destination = "/user/$userId/queue/messages"
        stompClient.subscribe(destination)

        return stompClient.messages
            .filter { it.headers["destination"] == destination }
            .mapNotNull { msg ->
                try {
                    messageAdapter.fromJson(msg.payload)
                } catch (e: Exception) {
                    null
                }
            }
    }

    override fun connectWebSocket(token: String) {
        stompClient.connect(authToken = token)
    }

    override fun disconnectWebSocket() {
        stompClient.disconnect()
    }
}
