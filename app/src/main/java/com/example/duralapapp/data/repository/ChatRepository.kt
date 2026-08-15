package com.example.duralapapp.data.repository

import com.example.duralapapp.data.api.ConversationApi
import com.example.duralapapp.data.api.MessageApi
import com.example.duralapapp.data.api.safeApiCall
import com.example.duralapapp.data.model.*
import com.example.duralapapp.data.websocket.StompWebSocketClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    val localMessageUpdates: SharedFlow<MessageResponse>
    fun notifyMessageSent(message: MessageResponse)

    suspend fun getMyConversations(): Result<List<ConversationResponse>>
    suspend fun getConversationById(id: String): Result<ConversationResponse>
    suspend fun getMessages(conversationId: String, page: Int = 0, size: Int = 30): Result<List<MessageResponse>>
    suspend fun sendMessage(conversationId: String, senderId: String, content: String): Result<MessageResponse>
    suspend fun sendMessageWithUuid(conversationId: String, senderId: String, content: String, clientMsgId: String): Result<MessageResponse>
    suspend fun syncMessages(sinceIso: String? = null): Result<List<MessageResponse>>
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus): Result<MessageResponse>
    suspend fun markAllAsRead(conversationId: String): Result<Unit>
    
    fun sendDeliveryAck(messageId: String)
    fun sendReadAck(messageId: String)

    fun observeConversationMessages(conversationId: String): Flow<MessageResponse>
    fun observeUserMessages(userId: String): Flow<MessageResponse>
    fun observeConversationUpdates(userId: String): Flow<ConversationUpdatedEvent>
    fun observeMessageStatusUpdates(conversationId: String): Flow<MessageStatusUpdatedEvent>
    fun observeUserStatusUpdates(userId: String): Flow<MessageStatusUpdatedEvent>
    fun observeUserPresence(userId: String): Flow<UserPresenceEvent>
    suspend fun getUserPresence(userId: String): Result<UserPresenceEvent>
    suspend fun getBatchPresence(userIds: List<String>): Result<List<UserPresenceEvent>>
    fun connectWebSocket(token: String)
    fun disconnectWebSocket()
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val conversationApi: ConversationApi,
    private val messageApi: MessageApi,
    private val presenceApi: com.example.duralapapp.data.api.PresenceApi,
    private val stompClient: StompWebSocketClient,
    private val moshi: Moshi
) : ChatRepository {

    private val _localMessageUpdates = MutableSharedFlow<MessageResponse>(extraBufferCapacity = 64)
    override val localMessageUpdates: SharedFlow<MessageResponse> = _localMessageUpdates.asSharedFlow()

    override fun notifyMessageSent(message: MessageResponse) {
        _localMessageUpdates.tryEmit(message)
    }

    private val messageAdapter by lazy { moshi.adapter(MessageResponse::class.java) }
    private val conversationUpdateAdapter by lazy { moshi.adapter(ConversationUpdatedEvent::class.java) }
    private val statusEventAdapter by lazy { moshi.adapter(MessageStatusUpdatedEvent::class.java) }
    private val presenceAdapter by lazy { moshi.adapter(UserPresenceEvent::class.java) }
    private val ackAdapter by lazy { moshi.adapter(MessageStatusUpdateRequest::class.java) }

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
        return sendMessageWithUuid(
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            clientMsgId = java.util.UUID.randomUUID().toString()
        )
    }

    override suspend fun sendMessageWithUuid(
        conversationId: String,
        senderId: String,
        content: String,
        clientMsgId: String
    ): Result<MessageResponse> {
        val result = safeApiCall {
            messageApi.sendMessage(
                MessageCreateRequest(
                    conversationId = conversationId,
                    senderId = senderId,
                    content = content,
                    clientMsgId = clientMsgId
                )
            )
        }
        result.onSuccess { msg ->
            _localMessageUpdates.tryEmit(msg)
        }
        return result
    }

    override suspend fun syncMessages(sinceIso: String?): Result<List<MessageResponse>> {
        return safeApiCall { messageApi.syncMessages(sinceIso) }
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus): Result<MessageResponse> {
        return safeApiCall { messageApi.updateMessageStatus(messageId, MessageStatusUpdateRequest(messageId, status)) }
    }

    override suspend fun markAllAsRead(conversationId: String): Result<Unit> {
        return safeApiCall { messageApi.markAllMessagesAsRead(conversationId) }
    }

    override fun sendDeliveryAck(messageId: String) {
        val payload = ackAdapter.toJson(MessageStatusUpdateRequest(messageId, MessageStatus.DELIVERED))
        stompClient.send("/app/chat.ack.delivery", payload)
    }

    override fun sendReadAck(messageId: String) {
        val payload = ackAdapter.toJson(MessageStatusUpdateRequest(messageId, MessageStatus.READ))
        stompClient.send("/app/chat.ack.read", payload)
    }

    override fun observeConversationMessages(conversationId: String): Flow<MessageResponse> {
        val topic = "/topic/conversation/$conversationId"
        stompClient.subscribe(topic)

        return stompClient.messages
            .filter { it.headers["destination"] == topic }
            .mapNotNull { msg ->
                try {
                    val parsed = messageAdapter.fromJson(msg.payload)
                    if (parsed != null) {
                        android.util.Log.d("DEBUG_STOMP", "[2] Repository emitted message | ConversationId: ${parsed.conversationId} | MessageId: ${parsed.id} | Content: ${parsed.content}")
                        _localMessageUpdates.tryEmit(parsed)
                    }
                    parsed
                } catch (e: Exception) {
                    android.util.Log.e("ChatRepositoryImpl", "Failed to deserialize real-time message payload for topic: $topic", e)
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
                    val parsed = messageAdapter.fromJson(msg.payload)
                    if (parsed != null) {
                        _localMessageUpdates.tryEmit(parsed)
                    }
                    parsed
                } catch (e: Exception) {
                    null
                }
            }
    }

    override fun observeConversationUpdates(userId: String): Flow<ConversationUpdatedEvent> {
        val destination = "/user/$userId/queue/conversations"
        stompClient.subscribe(destination)

        return stompClient.messages
            .filter { it.headers["destination"] == destination }
            .mapNotNull { msg ->
                try {
                    conversationUpdateAdapter.fromJson(msg.payload)
                } catch (e: Exception) {
                    android.util.Log.e("ChatRepositoryImpl", "Failed to deserialize conversation update for destination: $destination", e)
                    null
                }
            }
    }

    override fun observeMessageStatusUpdates(conversationId: String): Flow<MessageStatusUpdatedEvent> {
        val topic = "/topic/conversation/$conversationId"
        stompClient.subscribe(topic)

        return stompClient.messages
            .filter { it.headers["destination"] == topic }
            .mapNotNull { msg ->
                try {
                    statusEventAdapter.fromJson(msg.payload)
                } catch (e: Exception) {
                    null
                }
            }
    }

    override fun observeUserStatusUpdates(userId: String): Flow<MessageStatusUpdatedEvent> {
        val destination = "/user/$userId/queue/message-status"
        stompClient.subscribe(destination)

        return stompClient.messages
            .filter { it.headers["destination"] == destination }
            .mapNotNull { msg ->
                try {
                    statusEventAdapter.fromJson(msg.payload)
                } catch (e: Exception) {
                    null
                }
            }
    }

    override fun observeUserPresence(userId: String): Flow<UserPresenceEvent> {
        val topic = "/topic/presence/$userId"
        stompClient.subscribe(topic)

        return stompClient.messages
            .filter { it.headers["destination"] == topic }
            .mapNotNull { msg ->
                try {
                    presenceAdapter.fromJson(msg.payload)
                } catch (e: Exception) {
                    null
                }
            }
    }

    override suspend fun getUserPresence(userId: String): Result<UserPresenceEvent> {
        return safeApiCall { presenceApi.getUserPresence(userId) }
    }

    override suspend fun getBatchPresence(userIds: List<String>): Result<List<UserPresenceEvent>> {
        return safeApiCall { presenceApi.getBatchPresence(com.example.duralapapp.data.model.BatchPresenceRequest(userIds)) }
    }

    override fun connectWebSocket(token: String) {
        stompClient.connect(authToken = token)
    }

    override fun disconnectWebSocket() {
        stompClient.disconnect()
    }
}

