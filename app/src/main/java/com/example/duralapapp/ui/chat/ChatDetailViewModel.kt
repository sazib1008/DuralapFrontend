package com.example.duralapapp.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.local.PendingMessageItem
import com.example.duralapapp.data.local.PendingMessageQueue
import com.example.duralapapp.data.model.MessageCreateRequest
import com.example.duralapapp.data.model.MessageResponse
import com.example.duralapapp.data.model.MessageStatus
import com.example.duralapapp.data.model.MessageType
import com.example.duralapapp.data.model.MessageUiStatus
import com.example.duralapapp.data.network.TokenManager
import com.example.duralapapp.data.repository.ChatRepository
import com.example.duralapapp.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

sealed interface ChatDetailUiState {
    data object Loading : ChatDetailUiState
    data class Success(val messages: List<MessageResponse>, val currentUserId: String) : ChatDetailUiState
    data class Error(val message: String) : ChatDetailUiState
}

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager,
    private val syncManager: SyncManager,
    private val pendingQueue: PendingMessageQueue,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    val recipientName: String = savedStateHandle["recipientName"] ?: "Chat"

    val targetUserId: String
        get() {
            val state = _uiState.value
            if (state is ChatDetailUiState.Success) {
                val otherMsg = state.messages.firstOrNull { it.senderId != state.currentUserId }
                if (otherMsg != null) return otherMsg.senderId
            }
            return ""
        }

    private val _uiState = MutableStateFlow<ChatDetailUiState>(ChatDetailUiState.Loading)
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            tokenManager.userId.collect { id ->
                if (!id.isNullOrBlank()) {
                    currentUserId = id
                    loadMessages()
                    observeRealTimeMessages()
                    observeStatusUpdates()
                    observeSyncedMessages()
                }
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = ChatDetailUiState.Loading
            chatRepository.getMessages(conversationId)
                .onSuccess { msgList ->
                    _uiState.value = ChatDetailUiState.Success(
                        messages = msgList.reversed(),
                        currentUserId = currentUserId
                    )
                    chatRepository.markAllAsRead(conversationId)
                }
                .onFailure { error ->
                    _uiState.value = ChatDetailUiState.Error(
                        error.localizedMessage ?: "Failed to load messages"
                    )
                }
        }
    }

    private fun observeRealTimeMessages() {
        viewModelScope.launch {
            chatRepository.observeConversationMessages(conversationId)
                .collect { newMsg ->
                    val currentState = _uiState.value
                    android.util.Log.d("DEBUG_STOMP", "[3] ViewModel received message | ConversationId: $conversationId | MessageId: ${newMsg.id} | Current state class: ${currentState::class.simpleName}")
                    if (currentState is ChatDetailUiState.Success) {
                        val oldHash = System.identityHashCode(currentState)
                        val oldSize = currentState.messages.size
                        val existingIndex = currentState.messages.indexOfFirst { 
                            it.id == newMsg.id || (newMsg.clientMsgId != null && it.clientMsgId == newMsg.clientMsgId) 
                        }
                        val updatedList = if (existingIndex >= 0) {
                            currentState.messages.toMutableList().apply { this[existingIndex] = newMsg }
                        } else {
                            currentState.messages + newMsg
                        }
                        val newState = currentState.copy(messages = updatedList)
                        val newHash = System.identityHashCode(newState)
                        _uiState.value = newState
                        android.util.Log.d("DEBUG_STOMP", "[4] UiState updated | hashCode(old): $oldHash | hashCode(new): $newHash | Old count: $oldSize | New count: ${updatedList.size}")

                        if (newMsg.senderId != currentUserId) {
                            chatRepository.sendDeliveryAck(newMsg.id)
                            chatRepository.sendReadAck(newMsg.id)
                        }
                    } else {
                        android.util.Log.w("DEBUG_STOMP", "[3.1] ViewModel DROPPED real-time message because uiState is not Success! State: $currentState")
                    }
                }
        }
    }

    private fun observeStatusUpdates() {
        viewModelScope.launch {
            chatRepository.observeMessageStatusUpdates(conversationId)
                .collect { statusEvent ->
                    val currentState = _uiState.value
                    if (currentState is ChatDetailUiState.Success) {
                        val updatedList = currentState.messages.map { msg ->
                            if (msg.id == statusEvent.messageId) {
                                msg.copy(
                                    status = statusEvent.status,
                                    isRead = if (statusEvent.status == MessageStatus.READ) true else msg.isRead
                                )
                            } else msg
                        }
                        _uiState.value = currentState.copy(messages = updatedList)
                    }
                }
        }
    }

    private fun observeSyncedMessages() {
        viewModelScope.launch {
            syncManager.syncedMessages.collect { synced ->
                val convMessages = synced.filter { it.conversationId == conversationId }
                if (convMessages.isEmpty()) return@collect

                val currentState = _uiState.value
                if (currentState is ChatDetailUiState.Success) {
                    val currentMap = currentState.messages.associateBy { it.id }.toMutableMap()
                    convMessages.forEach { msg ->
                        currentMap[msg.id] = msg
                        if (msg.senderId != currentUserId) {
                            chatRepository.sendDeliveryAck(msg.id)
                        }
                    }
                    val sorted = currentMap.values.sortedBy { it.createdAt }
                    _uiState.value = currentState.copy(messages = sorted)
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || currentUserId.isBlank()) return

        val clientMsgId = UUID.randomUUID().toString()
        val timestamp = Instant.now()

        val optimisticMsg = MessageResponse(
            id = clientMsgId,
            conversationId = conversationId,
            senderId = currentUserId,
            content = content,
            messageType = MessageType.TEXT,
            mediaUrl = null,
            mediaType = null,
            fileName = null,
            fileSize = null,
            isRead = false,
            readAt = null,
            createdAt = timestamp,
            updatedAt = timestamp,
            senderInfo = null,
            clientMsgId = clientMsgId,
            status = MessageStatus.SENT
        )

        val currentState = _uiState.value
        if (currentState is ChatDetailUiState.Success) {
            _uiState.value = currentState.copy(
                messages = currentState.messages + optimisticMsg
            )
        }

        val request = MessageCreateRequest(
            conversationId = conversationId,
            senderId = currentUserId,
            content = content,
            clientMsgId = clientMsgId
        )
        pendingQueue.enqueue(PendingMessageItem(clientMsgId, request, MessageUiStatus.SENDING))

        viewModelScope.launch {
            chatRepository.sendMessageWithUuid(
                conversationId = conversationId,
                senderId = currentUserId,
                content = content,
                clientMsgId = clientMsgId
            ).onSuccess { sentMsg ->
                pendingQueue.markStatus(clientMsgId, MessageUiStatus.SENT)
                pendingQueue.remove(clientMsgId)
                val state = _uiState.value
                if (state is ChatDetailUiState.Success) {
                    val updated = state.messages.map { msg ->
                        if (msg.clientMsgId == clientMsgId || msg.id == clientMsgId) sentMsg else msg
                    }
                    _uiState.value = state.copy(messages = updated)
                }
            }.onFailure {
                pendingQueue.markStatus(clientMsgId, MessageUiStatus.FAILED)
            }
        }
    }

    fun retryMessage(clientMsgId: String) {
        val items = pendingQueue.getPendingItems()
        val target = items.firstOrNull { it.clientMsgId == clientMsgId } ?: return

        pendingQueue.markStatus(clientMsgId, MessageUiStatus.SENDING)
        viewModelScope.launch {
            chatRepository.sendMessageWithUuid(
                conversationId = target.request.conversationId,
                senderId = target.request.senderId,
                content = target.request.content,
                clientMsgId = clientMsgId
            ).onSuccess { sentMsg ->
                pendingQueue.markStatus(clientMsgId, MessageUiStatus.SENT)
                pendingQueue.remove(clientMsgId)
                val state = _uiState.value
                if (state is ChatDetailUiState.Success) {
                    val updated = state.messages.map { msg ->
                        if (msg.clientMsgId == clientMsgId || msg.id == clientMsgId) sentMsg else msg
                    }
                    _uiState.value = state.copy(messages = updated)
                }
            }.onFailure {
                pendingQueue.markStatus(clientMsgId, MessageUiStatus.FAILED)
            }
        }
    }
}

