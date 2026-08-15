package com.example.duralapapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.model.ConversationResponse
import com.example.duralapapp.data.model.MessageResponse
import com.example.duralapapp.data.model.UserInfo
import com.example.duralapapp.data.network.TokenManager
import com.example.duralapapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChatListUiState {
    data object Loading : ChatListUiState
    data class Success(val conversations: List<ConversationResponse>, val currentUserId: String = "") : ChatListUiState
    data class Error(val message: String) : ChatListUiState
    data object Empty : ChatListUiState
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _presenceMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val presenceMap: StateFlow<Map<String, Boolean>> = _presenceMap.asStateFlow()

    private val activePresenceJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private var currentUserId: String = ""

    init {
        observeUserId()
        loadConversations()
        observeLocalMessages()
        initWebSocketConnection()
    }

    private fun observeUserId() {
        viewModelScope.launch {
            tokenManager.userId.collect { id ->
                if (!id.isNullOrBlank()) {
                    currentUserId = id
                    observeRealTimeUserMessages(id)
                    observeRealTimeConversationUpdates(id)
                }
            }
        }
    }

    fun loadConversations(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent && _uiState.value !is ChatListUiState.Success) {
                _uiState.value = ChatListUiState.Loading
            }
            chatRepository.getMyConversations()
                .onSuccess { conversations ->
                    val sorted = conversations.sortedByDescending { it.lastMessage?.createdAt ?: it.createdAt }
                    if (sorted.isEmpty()) {
                        _uiState.value = ChatListUiState.Empty
                    } else {
                        _uiState.value = ChatListUiState.Success(
                            conversations = sorted,
                            currentUserId = currentUserId
                        )
                        fetchAndObservePresenceForConversations(sorted)
                    }
                }
                .onFailure { error ->
                    if (!silent || _uiState.value !is ChatListUiState.Success) {
                        val errorMessage = when (error) {
                            is retrofit2.HttpException -> {
                                when (error.code()) {
                                    401 -> "Session expired. Please log in again."
                                    403 -> "Access denied. Unauthorized request."
                                    else -> "Server error (${error.code()}). Please try again."
                                }
                            }
                            is java.io.IOException -> "Network timeout. Please check your connection."
                            else -> error.localizedMessage ?: "Failed to load conversations"
                        }
                        _uiState.value = ChatListUiState.Error(errorMessage)
                    }
                }
        }
    }

    fun refreshConversations() {
        loadConversations(silent = true)
    }

    private fun updateConversationInState(
        conversationId: String,
        content: String,
        timestamp: java.time.Instant,
        senderId: String? = null,
        senderInfo: com.example.duralapapp.data.model.UserInfo? = null,
        isRead: Boolean = false
    ) {
        val currentState = _uiState.value
        if (currentState is ChatListUiState.Success) {
            val existingIndex = currentState.conversations.indexOfFirst { it.id == conversationId }
            if (existingIndex >= 0) {
                val oldConv = currentState.conversations[existingIndex]
                val updatedMsg = oldConv.lastMessage?.copy(
                    content = content,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    senderId = senderId ?: oldConv.lastMessage.senderId,
                    senderInfo = senderInfo ?: oldConv.lastMessage.senderInfo,
                    isRead = isRead
                ) ?: com.example.duralapapp.data.model.MessageResponse(
                    id = java.util.UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    senderId = senderId ?: "",
                    content = content,
                    messageType = com.example.duralapapp.data.model.MessageType.TEXT,
                    mediaUrl = null,
                    mediaType = null,
                    fileName = null,
                    fileSize = null,
                    isRead = isRead,
                    readAt = null,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    senderInfo = senderInfo,
                    clientMsgId = null,
                    status = com.example.duralapapp.data.model.MessageStatus.SENT
                )
                val updatedConv = oldConv.copy(lastMessage = updatedMsg)
                val mutableList = currentState.conversations.toMutableList()
                mutableList[existingIndex] = updatedConv
                val reSorted = mutableList.sortedByDescending { it.lastMessage?.createdAt ?: it.createdAt }
                _uiState.value = currentState.copy(conversations = reSorted)
            }
        }
        // Silently reload from repository to stay completely up to date
        loadConversations(silent = true)
    }

    private fun observeRealTimeUserMessages(userId: String) {
        viewModelScope.launch {
            chatRepository.observeUserMessages(userId)
                .catch { /* ignore network error in bg flow */ }
                .collect { msg ->
                    updateConversationInState(
                        conversationId = msg.conversationId,
                        content = msg.content,
                        timestamp = msg.createdAt,
                        senderId = msg.senderId,
                        senderInfo = msg.senderInfo,
                        isRead = (msg.senderId == currentUserId)
                    )
                }
        }
    }

    private fun observeRealTimeConversationUpdates(userId: String) {
        viewModelScope.launch {
            chatRepository.observeConversationUpdates(userId)
                .catch { /* ignore network error in bg flow */ }
                .collect { event ->
                    updateConversationInState(
                        conversationId = event.conversationId,
                        content = event.lastMessageContent,
                        timestamp = event.lastMessageAt,
                        senderId = event.lastMessageSenderId
                    )
                }
        }
    }

    private fun observeLocalMessages() {
        viewModelScope.launch {
            chatRepository.localMessageUpdates
                .collect { msg ->
                    updateConversationInState(
                        conversationId = msg.conversationId,
                        content = msg.content,
                        timestamp = msg.createdAt,
                        senderId = msg.senderId,
                        senderInfo = msg.senderInfo,
                        isRead = (msg.senderId == currentUserId)
                    )
                }
        }
    }

    private fun fetchAndObservePresenceForConversations(conversations: List<ConversationResponse>) {
        val otherUserIds = conversations.mapNotNull { conv ->
            conv.participantIds.firstOrNull { it != currentUserId }
                ?: conv.participants?.firstOrNull { it.id != currentUserId }?.id
        }.distinct()

        if (otherUserIds.isEmpty()) return

        // 1. Initial REST batch presence query
        viewModelScope.launch {
            chatRepository.getBatchPresence(otherUserIds)
                .onSuccess { presences ->
                    val map = presences.associate { it.userId to (it.status == com.example.duralapapp.data.model.UserStatus.ONLINE) }
                    _presenceMap.value = _presenceMap.value + map
                }
        }

        // 2. Real-time WebSocket presence streams
        otherUserIds.forEach { userId ->
            if (!activePresenceJobs.containsKey(userId)) {
                val job = viewModelScope.launch {
                    chatRepository.observeUserPresence(userId)
                        .catch { /* ignore */ }
                        .collect { event ->
                            val isOnline = (event.status == com.example.duralapapp.data.model.UserStatus.ONLINE)
                            _presenceMap.value = _presenceMap.value + (event.userId to isOnline)
                        }
                }
                activePresenceJobs[userId] = job
            }
        }
    }

    private fun initWebSocketConnection() {
        viewModelScope.launch {
            tokenManager.accessToken.collect { token ->
                if (!token.isNullOrBlank()) {
                    chatRepository.connectWebSocket(token)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activePresenceJobs.values.forEach { it.cancel() }
        activePresenceJobs.clear()
        chatRepository.disconnectWebSocket()
    }
}

