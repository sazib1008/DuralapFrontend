package com.example.duralab.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralab.data.local.dao.MessageDao
import com.example.duralab.data.local.entity.MessageEntity
import com.example.duralab.data.remote.MessageApi
import com.example.duralab.data.remote.StompClient
import com.example.duralab.util.TokenManager
import com.example.duralab.util.UiState
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val stompClient: StompClient,
    private val messageApi: MessageApi,
    private val tokenManager: TokenManager,
    private val moshi: Moshi
) : ViewModel() {

    private val _messages = MutableStateFlow<UiState<List<MessageEntity>>>(UiState.Loading)
    val messages: StateFlow<UiState<List<MessageEntity>>> = _messages

    private var currentChatId: String? = null

    init {
        // Start listening to WebSocket messages globally for the chat
        viewModelScope.launch {
            stompClient.messages.collect { messagePayload ->
                handleIncomingStompMessage(messagePayload)
            }
        }
    }

    fun setChatId(chatId: String) {
        currentChatId = chatId
        val currentUserId = tokenManager.getUserId() ?: ""
        
        viewModelScope.launch {
            // Load local messages
            messageDao.getMessagesForChat(chatId).collect {
                _messages.value = UiState.Success(it)
            }
        }
        
        // Sync API logic updating messages using the new MessageResponse mapping
        viewModelScope.launch {
            try {
                if (currentUserId.isNotEmpty()) {
                    val response = messageApi.getMessages(chatId)
                    if (response.isSuccessful) {
                        response.body()?.forEach { msgRsp ->
                            val msgTimestamp = try {
                                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.US)
                                format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                format.parse(msgRsp.createdAt)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                try {
                                    val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                                    format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    format.parse(msgRsp.createdAt)?.time ?: System.currentTimeMillis()
                                } catch (e2: Exception) {
                                    System.currentTimeMillis()
                                }
                            }
                            
                            val msgEntity = MessageEntity(
                                id = msgRsp.id,
                                chatId = msgRsp.conversationId,
                                senderId = msgRsp.senderId,
                                content = msgRsp.content,
                                timestamp = msgTimestamp,
                                status = if (msgRsp.isRead) "READ" else "SENT",
                                type = msgRsp.messageType.name
                            )
                            messageDao.insertMessage(msgEntity)
                            
                            // Mark as read immediately on fetch if unread and not from me
                            if (msgRsp.senderId != currentUserId && !msgRsp.isRead) {
                                messageApi.markMessageAsRead(msgRsp.id, currentUserId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Connect and subscribe to real-time updates for this chat
        val wsUrl = "wss://api.duralab.com/websocket"
        stompClient.connect(wsUrl)
        stompClient.subscribe("/topic/chat/$chatId", "sub-$chatId")
    }

    private fun handleIncomingStompMessage(payload: String) {
        try {
            val bodyStartIndex = payload.indexOf("\n\n")
            if (bodyStartIndex != -1) {
                var jsonBody = payload.substring(bodyStartIndex + 2)
                jsonBody = jsonBody.substringBefore('\u0000').trim()

                // Depending on Stomp setup, standard incoming messages match MessageEntity directly or MessageResponse format
                val adapter = moshi.adapter(MessageEntity::class.java)
                val incomingMessage = adapter.fromJson(jsonBody)
                
                if (incomingMessage != null) {
                    viewModelScope.launch {
                        messageDao.insertMessage(incomingMessage)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMessage(chatId: String, content: String) {
        val currentUserId = tokenManager.getUserId() ?: ""
        if (currentUserId.isEmpty()) return
        
        viewModelScope.launch {
            val newMessage = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = currentUserId,
                content = content,
                timestamp = System.currentTimeMillis(),
                status = "SENT",
                type = "TEXT"
            )
            // Insert locally first for fast UI update
            messageDao.insertMessage(newMessage)
            
            // Send via WebSocket mapping 
            val adapter = moshi.adapter(MessageEntity::class.java)
            val jsonPayload = adapter.toJson(newMessage)
            stompClient.send("/app/chat/$chatId", jsonPayload)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stompClient.disconnect()
    }
}
